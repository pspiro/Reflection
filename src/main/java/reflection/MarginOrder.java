package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.HashMap;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder;
import com.ib.client.DualOrder.DualParent;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import common.Alerts;
import common.Util;
import reflection.MarginTrans.GoodUntil;
import tw.util.S;
import web3.Stablecoin;
import web3.StockToken;

class MarginOrder extends JsonObject implements DualParent {
	double feePct;
	double lastBuffer;  // as percent, try 
	double bidBuffer;
	double maxLeverage;
	double minUserAmt = 100;
	double maxUserAmt = 200;
	
	enum Status {
		NeedPayment,		// waiting for blockchain payment transaction to complete; we may or may not have transaction hash
		StartedPayment, 
		ReceivedPayment,
		Buy,			// change to submitted buy
		Sell,			// primary order has filled, we are now monitoring sell orders
		Completed,
		Canceled
	}

	// move all this into a separate sub-object
	// field specified by user
	String wallet() { return getString( "wallet_public_key"); }
	String orderId() { return getString( "orderId"); }
	int conid() { return getInt( "conid"); }
	Action action() throws Exception { return getEnum( "action", Action.values() ); }
	double amtToSpend() { return getDouble( "amountToSpend"); }
	double leverage() { return getDouble( "leverage"); }
	double entryPrice() { return getDouble( "entryPrice"); }
	double profitTakerPrice() { return getDouble( "profitTakerPrice"); }
	double stopLossPrice() { return getDouble( "stopLossPrice"); }
	String goodUntil() { return getString( "goodUntil"); }
	String currency() { return getString( "currency"); }
	String completedHow() { return getString( "completedHow"); }
	
	// status fields set by us; these will be sent to Frontend and must be ignored
	Status status() { return getEnum( "status", Status.values(), Status.NeedPayment); }
	// also hash and receipt

	// transient, non-serializeable 
	private ApiController m_conn;
	private Stocks m_stocks;
	private int m_roundedQty;  // you need to save this or change them to methods. pas
	private double m_desiredQty; // you need to save this. pas

	//private HashMap<String,OrdStatus> m_ibMap = new HashMap<>(); // map permId to status of all IB orders, buy and sell


	// the orders
	private DualOrder m_entryOrder;
	private DualOrder m_profitOrder;
	private DualOrder m_stopOrder;

	/** Called when order is received from Frontend */
	MarginOrder(
			ApiController conn,
			Stocks stocks,
			String wallet,
			String orderId,
			int conid,
			Action action,
			double amt,
			double leverage,
			double entryPrice,
			double profitTakerPrice,
			double stopPrice,
			GoodUntil goodUntil,
			String currency) {
		
		this( conn, stocks);
		
		put( "wallet_public_key", wallet.toLowerCase() );
		put( "orderId", orderId); 
		put( "conid", conid);
		put( "action", action); 
		put( "amountToSpend", amt);
		put( "leverage", leverage);
		put( "entryPrice", entryPrice);
		put( "profitTakerPrice", profitTakerPrice);
		put( "stopLossPrice", stopPrice);
		put( "goodUntil", goodUntil);
		put( "currency", currency);

		status( Status.NeedPayment);  // waiting for blockchain payment transaction
	}			

	/** Called when order is restore from MarginStore */
	MarginOrder( ApiController conn, Stocks stocks) {
		m_conn = conn;
		m_stocks = stocks;
	}			

	/** receive live orders only for this MarginOrder 
	 * @param orderRefMap 
	 * @throws Exception */ 
	public void onReconnected( HashMap<Integer,LiveOrder> permIdMap, HashMap<String, LiveOrder> orderRefMap) throws Exception {
		// fields of ordStatus are: 
//		"permId"
//		"action"
//		"filled"
//		"avgPrice"
		
		// update the saved live orders with the new set of live orders in case the status
		// or filled amount changed during the restart
		for (var liveOrder : orderRefMap.values() ) {
			String orderId = liveOrder.orderRef().split( " ")[0]; // get Margin order orderId from order ref
			if (orderId.equals( orderId() ) ) {
				Util.wrap( () -> updateOrderStatus( 
						liveOrder.permId(),
						liveOrder.action(),
						liveOrder.filled(),
						liveOrder.avgPrice()
						) );
			}
		}
		
		switch( status() ) {
			case NeedPayment:
				acceptPayment();
				break;
				
			case StartedPayment:
				// this means we were waiting for payment and we don't know if we got it or not
				// should be rare, will only happen if RefAPI resets while waiting for payment
				// we can improve this and figure out if payment was received if desired. pas
				Alerts.alert(
						"RefAPI", 
						"MARGIN_ORDER PAYMENT IS LOST, HANDLE THIS", 
						S.format( "orderId=%s", orderId() ) );
				break;
				
			case ReceivedPayment:
			case Buy:
				// waiting for buy order to fill
				// we may or may not have placed the buy order yet
				// there should be an open buy order
				// if there is not, we have to find out what happened to buy order that
				// was open when the RefAPI reset; it was either filled or canceled
				placeBuyOrder( orderRefMap);
				break;
				
			case Sell:
				placeSellOrders( orderRefMap);
				break;
				
			case Completed:
			case Canceled:  // nothing to do
				break;
		}
			
	}

	public void acceptPayment() throws Exception {
		Util.require( status() == Status.NeedPayment, "Invalid status %s to accept payment", status() );
		
		status( Status.StartedPayment);
		
		out( "Accepting payment");
		
		// get current receipt balance
		String walletAddr = wallet();
		double amtToSpend = amtToSpend();
		StockToken receipt = m_stocks.getReceipt();
		double prevBalance = receipt.getPosition( walletAddr);  // or get from HookServer

		// wrong, don't pull config from Main. pas
		Stablecoin stablecoin = currency().equals( m_config.rusd().name() ) ? m_config.rusd() : m_config.busd();

		// transfer the crypto to RefWallet and give the user a receipt; it would be good if we can find a way to tie the receipt to this order
		String hash = m_config.rusd().buyStock(walletAddr, stablecoin, amtToSpend, receipt, amtToSpend)
				.waitForHash();

		// make this an order log entry instead
		
		out( "Accepted payment with trans hash %s", hash);
		
		// update transaction hash on MarginOrder
		transHash( hash);
		save();
		
		// wait for receipt to register because a transaction hash is not a guarantee of success;
		// alternatively we could wait for some other type of blockchain finality
		int retVal = Util.waitFor( 60, () -> receipt.getPosition( 
				walletAddr) - prevBalance >= amtToSpend - .01);
		
		if (retVal < 0) {
			// user did not get a receipt in one minute
			// this should never happen and someone needs to investigate
			Alerts.alert( "Reflection", "USER DID NOT RECEIVE RECEIPT FOR MARGIN ORDER", 
					String.format( "wallet=%s  orderId=%s", walletAddr, orderId() ) );

			put( "completedHow", String.format( 
					"Canceled by user; status was %s", status() ) );
			
			status( Status.Canceled);
			return;
		}

		out( "Confirmed receipt balance in %s seconds", retVal);
		
		// note that we got the receipt; use a status for this instead?
		gotReceipt( true);
		save();
		
		// check if order has been canceled by user while we were waiting for blockchain transaction and receipt
		if ( status() == Status.StartedPayment) {
			placeBuyOrder( null);
			save();
		}

		// NOTE: there is a window here. If RefAPI terminates before the blockchain transaction completes,
		// when it restarts we won't know for sure if the transaction was successful or not;
		// operator assistance would be required		
	}


	/** really place or restore buy order 
	 * @throws Exception */
	void placeBuyOrder( HashMap<String,LiveOrder> liveOrders) throws Exception {
		
		Util.require( status() == Status.ReceivedPayment || status() == Status.Buy, 
				"Invalid status %s when placing margin order", status() );
		
		out( "***Placing margin entry orders");
		
		status( Status.Buy);
		
		double feePct = .01; // fix this. pas
		double totalSpend = amtToSpend() * leverage() * (1. - feePct);
		m_desiredQty = totalSpend / entryPrice();
		m_roundedQty = OrderTransaction.positionTracker.buyOrSell( conid(), true, m_desiredQty, 1);
		
		// if we are restoring, and there is no live order, you have to subtract
		// out the filled amount and place for the remaining size only. pas
		
		int remainingQty = m_roundedQty - totalBought();
		
		// place day and night orders
		m_entryOrder = new DualOrder( m_conn, null, "ENTRY", orderId() + " entry", this); 
		m_entryOrder.action( Action.Buy);
		m_entryOrder.quantity( remainingQty);
		m_entryOrder.orderType( OrderType.LMT);
		m_entryOrder.lmtPrice( entryPrice() );
		m_entryOrder.tif( TimeInForce.GTC);   // must consider this
		m_entryOrder.outsideRth( true);

		try {
			m_entryOrder.placeOrder( conid(), liveOrders);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override public synchronized void onFilled(
			DualOrder dualOrd, 
			int permId, 
			Action action, 
			int filled, 
			double avgPrice) {
		
		Util.wrap( () -> {
			// add order to or update order in IB order map
			updateOrderStatus(
					permId,
					action,
					filled,
					avgPrice);
			
			Status m_status = status();
			
			S.out( "MarginOrder received fill  name=%s  status=%s", dualOrd.name(), m_status);
	
			if (m_status == Status.Buy) {
				if (totalBought() >= m_roundedQty) {
					S.out( "  entry order is filled");
					m_status = Status.Sell;
					cancelBuyOrders();
	//				placeSellOrders();
				}
			}
			else if (m_status == Status.Sell) {
				if (totalSold() >= m_roundedQty) {
					S.out( "  sell orders have filled");
					m_status = Status.Completed;
					cancelSellOrders();
					// now we need to cash out the user; they must initiate this
				}
			}
			else {
				out( "  error");
			}
			
			save();
		});
		
	}
	
	void placeSellOrders(HashMap<String,LiveOrder> liveOrders) {
		if (profitTakerPrice() > 0) {
			placeProfitTaker( liveOrders);
		}

		// we must set our own stop loss price which is >= theirs
		if (stopLossPrice() > 0) {
			placeStopLoss( liveOrders);
		}
	}			
	
	private void placeProfitTaker(HashMap<String,LiveOrder> liveOrders) {
		out( "***Placing margin profit-taker orders");
		
		m_profitOrder = new DualOrder( m_conn, null, "PROFIT", orderId() + " profit", this);
		m_profitOrder.action( Action.Sell);
		m_profitOrder.quantity( m_roundedQty);
		m_profitOrder.orderType( OrderType.LMT);
		m_profitOrder.lmtPrice( profitTakerPrice() );
		m_profitOrder.tif( TimeInForce.GTC);   // must consider this
		m_profitOrder.outsideRth( true);
		m_profitOrder.orderRef( orderId() + " profit" );

		try {
			m_profitOrder.placeOrder( conid(), liveOrders);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void placeStopLoss(HashMap<String,LiveOrder> liveOrders) {
		out( "***Placing margin stop-loss orders");
		
		// need to check m_filledPrimary; if < roundedQty, we need to adjust the size here. pas
		m_stopOrder = new DualOrder( 
				m_conn, 
				m_stocks.getStockByConid( conid() ).prices(), 
				"STOP", 
				orderId() + 
				" stop", 
				this);
		
		m_stopOrder.action( Action.Sell);
		m_stopOrder.quantity( m_roundedQty);
		m_stopOrder.orderType( OrderType.STP_LMT);  // use STOP_LMT  because STOP cannot be set to trigger outside RTH
		m_stopOrder.lmtPrice( stopLossPrice() * .95);
		m_stopOrder.stopPrice( stopLossPrice() );
		m_stopOrder.tif( TimeInForce.GTC);   // must consider this
		m_stopOrder.outsideRth( true);
		m_stopOrder.orderRef( orderId() + " stop" );
		// m_stopOrder.ocaGroup( orderId() + " bracket" );
		
		try {
			m_stopOrder.placeOrder( conid(), liveOrders);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}  // you need to cancel one when the other one fills; make sure stop is > liq order or place your own stop at the liq price
	
	void cancelBuyOrders() {
		if (m_entryOrder != null) {
			out( "canceling buy orders");
			m_entryOrder.cancel();
		}
	}

	private void cancelSellOrders() {
		if (m_profitOrder != null) {
			out( "canceling profit orders");
			m_profitOrder.cancel();
		}
		
		if (m_stopOrder != null) {
			out( "canceling stop orders");
			m_stopOrder.cancel();
		}
	}
	
	private void save() {
		// save, restore 
	}

	public void transHash(String hash) {
		put( "transHash", hash);
		
	}

	public void gotReceipt(boolean v) {
		put( "gotReceipt", v);
	}

	public void userCancel() throws RefException {
		switch( status() ) {
		case NeedPayment:
		case StartedPayment:
		case ReceivedPayment:
		case Buy:
			cancelByUser();
			break;

		case Sell:
			require( false, RefCode.CANT_CANCEL, "It's too late to cancel; the buy order has already been filled");
			break;

		case Completed:
			require( false, RefCode.CANT_CANCEL, "It's too late to cancel; the order has already completed");
			break;
			
		case Canceled:
			require( false, RefCode.CANT_CANCEL, "The order has already been canceled");
			break;
		}
	}

	/** Payment may or may not have been accepted.
	 *  Buy orders may or may not have been placed. */
	private void cancelByUser() {
		put( "completedHow", String.format( 
				"Canceled by user; status was %s", status() ) );
		
		cancelBuyOrders();

		status( Status.Canceled);
	}
	
	// called when the order is restored from the db
	synchronized void process() throws Exception {
//			if (m_status == Status.Entry) {
//				placeBuyOrder();
//			}

		// check for liquidation
//			if (balance < 0) {
//				double loan = -balance;
//				
//				if (prices.bid() <= 0) {
//					out( "liquidating  no bid");
//					liquidate( conn);
//				}
//				else {
//					double bidCollat = prices.bid() * position;
//					double bidCoverage = bidCollat / loan; 
//					if (bidCoverage <= Main.m_config.bidLiqBuffer() ) {
//						out( "liquidate bid=%s  bidCollat=%s  loan=%s  bidCovererage=%s", 
//								prices.bid(), bidCollat, loan, bidCoverage);
//						liquidate( conn);
//					}
//					
//					if (prices.validAsk() ) {
//						double markCollat = prices.askMark() * position;
//						double markCoverage = markCollat / loan;
//						if (markCoverage <= Main.m_config.markLiqBuffer() ) {
//							out( "liquidate bid=%s  ask=%s  last=%s  markCollat=%s  loan=%s  markCovererage=%s", 
//									prices.bid(), prices.ask(), prices.ask(), markCollat, loan, markCoverage);
//						}
//					}
//				}
//			}
		
	}
	
	static class OrdStatus extends JsonObject {
		OrdStatus( String permId, Action action) {
			put( "permId", permId);
			put( "action", action);
		}
		
		void update( int filled, double avgPrice) {
			put( "filled", filled);
			put( "avgPrice", avgPrice);
		}
	}
	
	private int totalBought() {
		return total( Action.Buy);
	}
	
	private int totalSold() {
		return total( Action.Sell);
	}

	private int total(Action action) {
		try {
			int sum = 0;
			for (var item : orderMap().values() ) {
				JsonObject ord = (JsonObject)item;
				sum += ord.getString( "action").equals( action.toString() ) ? ord.getInt( "filled") : 0;
			}
			return sum;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;  // should never happen
		}
	}

	private void out( String format, Object... params) {
		S.out( orderId() + " " + format, params);
	}
	
	void status( Status status) {
		put( "status", status);
	}

	/** maps permId to OrdStatus; 
	 *  saves the status of all IB orders that have been placed or filled
	 *  for this MarginOrder
	 *  
	 * it would be so much better if this could be a map of id->OrdStatus object
	 *  
	 * @throws Exception */  // create a JsonMap object, same as JsonObject but any type. pas
	JsonObject orderMap() throws Exception {  // could we cast this to HashMap<String,JsonObject>? pas
		return getObject( "orderMap");
	}
	
	void updateOrderStatus( int permId, Action action, int filled, double avgPrice) throws Exception {
		getOrCreateOrderStatus( permId, action)
			.putAll( Util.toJson( "filled", filled, "avgPrice", avgPrice) );
	}
	
	JsonObject getOrCreateOrderStatus( int permId, Action action) throws Exception {
		return (JsonObject)Util.getOrCreate( 
				orderMap(), 
				"" + permId,
				() -> Util.toJson( "permId", permId, "action", action) );
	}
}
// implement save()
// open items
// check for fill during reset, i.e. live savedOrder that is not restored
// liquidation
// test single stop order
// test dual stop orders
// implement cancel, user sell/liquidation, user modify stop/profit/entry prices
// auto-liq at COB regular hours if market is closed next day
// do not accept order during extended hours if market is closed next day
// build an index to return wallet order efficiently
// get leach MarginOrder hold a ptr to Prices so you don't have to look it up in MarginTrans.getOrders()
// test canceling at all different states
// detect order being canceled
// handle failure when placing initial ib order
// handle system cancel event; replace order when appropriate
// let canceled orders remain for some time and then get cleared out; user must withdraw the cash
