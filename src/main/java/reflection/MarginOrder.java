package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.HashMap;
import java.util.function.Consumer;

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

public class MarginOrder extends JsonObject implements DualParent {
	double feePct;
	double lastBuffer;  // as percent, try 
	double bidBuffer;
	double maxLeverage;
	double minUserAmt = 100;
	double maxUserAmt = 200;
	
	public enum Status {
		NeedPayment,		// waiting for blockchain payment transaction to complete; we may or may not have transaction hash
		InitiatedPayment,		// submitted transaction; may or may not have trans hash and receipt
		GotReceipt,
		PlacedBuyOrder,
		BuyOrderFilled,			// primary order has filled, we are now monitoring sell orders
		PlacedSellOrders,
		Completed,
		Canceled,  
	}

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
	double desiredQty() { return getDouble( "desiredQty"); } // could calc this instead of storing it
	int roundedQty() { return getInt( "roundedQty"); }
	Status status() { return getEnum( "status", Status.values(), Status.NeedPayment); } // status fields set by us; these will be sent to Frontend and must be ignored
	boolean gotReceipt() { return getBool( "gotReceipt"); }

	// order state fields:
	// orderId
	// action
	// filled
	// avgPrice
	
	
	// other fields:
//	hash
//	receipt
//	loanAmt, 
//	liquidationPrice, 
//	value (of position + cash)   
//	bidPrice,		// done 
//	askPrice, 		// done
//	sharesHeld,   // done
//	sharesToBuy,  // done
//	symbol,       // done
	
	// transient, non-serializeable 
	private final ApiController m_conn;
	private final Stocks m_stocks;
	private final MarginStore m_store;
	private final Consumer<Prices> m_listener = prices -> updateBidAsk( prices);
			
	// the orders
	private DualOrder m_entryOrder;
	private DualOrder m_profitOrder;
	private DualOrder m_stopOrder;

	/** Called when order is received from Frontend */
	MarginOrder(
			ApiController conn,
			Stocks stocks,
			MarginStore store,
			String wallet,
			String orderId,
			int conid,
			Action action,
			double amtToSpend,
			double leverage,
			double entryPrice,
			double profitTakerPrice,
			double stopPrice,
			GoodUntil goodUntil,
			String currency) {
		
		this( conn, stocks, store);
		
		put( "wallet_public_key", wallet.toLowerCase() );
		put( "orderId", orderId); 
		put( "conid", conid);
		put( "action", action); 
		put( "amountToSpend", amtToSpend);
		put( "leverage", leverage);
		put( "entryPrice", entryPrice);
		put( "profitTakerPrice", profitTakerPrice);
		put( "stopLossPrice", stopPrice);
		put( "goodUntil", goodUntil);
		put( "currency", currency);
		put( "symbol", stock().symbol() );
		
		double feePct = .01; // fix this. pas
		double totalSpend = amtToSpend() * leverage() * (1. - feePct);
		put( "desiredQty", totalSpend / entryPrice() );
		put( "roundedQty", OrderTransaction.positionTracker.buyOrSell( conid(), true, desiredQty(), 1) );
		put( "sharesHeld", 0);
		put( "sharesToBuy", desiredQty() );
		put( "loanAmt", 0);
		put( "orderMap", new JsonObject() );

		status( Status.NeedPayment);  // waiting for blockchain payment transaction

		prices().addListener( m_listener);
	}			

	/** Called when order is restore from MarginStore 
	 * @param store */
	MarginOrder( ApiController conn, Stocks stocks, MarginStore store) {
		m_conn = conn;
		m_stocks = stocks;
		m_store = store;
		
	}

	/** Called only for orders restored from disk */ 
	public void onReconnected( HashMap<Integer,LiveOrder> permIdMap, HashMap<String, LiveOrder> orderRefMap) {
		out( "onReconnected()");
		
		prices().addListener( m_listener);
		
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
				
			case InitiatedPayment:
				// this means we were waiting for payment and we don't know if we got it or not
				// should be rare, will only happen if RefAPI resets while waiting for payment
				// we can improve this and figure out if payment was received if desired. pas
				Alerts.alert(
						"RefAPI", 
						"MARGIN_ORDER PAYMENT IS LOST, HANDLE THIS", 
						S.format( "orderId=%s", orderId() ) );
				
				cancel( "Order payment was lost");
				break;
				
			case GotReceipt:
				// waiting for buy order to fill
				// we may or may not have placed the buy order yet
				// there should be an open buy order
				// if there is not, we have to find out what happened to buy order that
				// was open when the RefAPI reset; it was either filled or canceled
				placeBuyOrder( orderRefMap);
				break;
				
			case PlacedBuyOrder:  // waiting for buy orders to fill
				break;
			
			case BuyOrderFilled:
				placeSellOrders( orderRefMap);
				break;

			case PlacedSellOrders:
			case Completed:
			case Canceled:  // nothing to do
				break;
		}
			
	}

	public void acceptPayment() {
		try {
			acceptPayment_();
		}
		catch (Exception e) {
			e.printStackTrace();
			
			cancel( "Failed to accept payment - " + e.getMessage() );
		}
	}
	
	public void acceptPayment_() throws Exception  {
		Util.require( status() == Status.NeedPayment, "Invalid status %s to accept payment", status() );
		
		out( "Accepting payment");
		
		// get current receipt balance
		String walletAddr = wallet();
		double amtToSpend = amtToSpend();
		StockToken receipt = m_stocks.getReceipt();
		double prevBalance = receipt.getPosition( walletAddr);  // or get from HookServer

		// wrong, don't pull config from Main. pas
		Stablecoin stablecoin = currency().equals( m_config.rusd().name() ) ? m_config.rusd() : m_config.busd();

		status( Status.InitiatedPayment);
		save();

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
			out( "Receipt was not received by wallet");
			
			// user did not get a receipt in one minute
			// this should never happen and someone needs to investigate
			Alerts.alert( "Reflection", "USER DID NOT RECEIVE RECEIPT FOR MARGIN ORDER", 
					String.format( "wallet=%s  orderId=%s", walletAddr, orderId() ) );
			
			throw new Exception( "Received transaction hash but could not confirm receipt");
		}

		out( "Confirmed receipt balance in %s seconds", retVal);
		
		// note that we got the receipt; use a status for this instead?
		gotReceipt( true);
		save();
		
		// if order has been canceled by user while we were waiting, bail out
		if ( status() == Status.Canceled) {
			return;
		}
		
		status( Status.GotReceipt);
		save();
		
		placeBuyOrder( null);

		// NOTE: there is a window here. If RefAPI terminates before the blockchain transaction completes,
		// when it restarts we won't know for sure if the transaction was successful or not;
		// operator assistance would be required		
	}


	/** really place or restore buy order 
	 * @throws Exception */
	void placeBuyOrder( HashMap<String,LiveOrder> liveOrders) {
		try {
			placeBuyOrder_( liveOrders);
		}
		catch (Exception e) {
			e.printStackTrace();

			cancel( "Failed to place buy orders - " + e.getMessage() );
		}
	}
	
	private void placeBuyOrder_( HashMap<String,LiveOrder> liveOrders) throws Exception {
		
		Util.require( status() == Status.GotReceipt, "Invalid status %s when placing buy order", status() );
		
		out( "***Placing margin entry orders");
		
		// if we are restoring, and there is no live order, you have to subtract
		// out the filled amount and place for the remaining size only. pas
		
		int remainingQty = roundedQty() - totalBought();
		
		// place day and night orders
		m_entryOrder = new DualOrder( m_conn, null, "ENTRY", orderId() + " entry", this); 
		m_entryOrder.action( Action.Buy);
		m_entryOrder.quantity( remainingQty);
		m_entryOrder.orderType( OrderType.LMT);
		m_entryOrder.lmtPrice( entryPrice() );
		m_entryOrder.tif( TimeInForce.GTC);   // must consider this
		m_entryOrder.outsideRth( true);

		m_entryOrder.placeOrder( conid(), liveOrders);

		status( Status.PlacedBuyOrder);
		save();

		// now we wait for the buy order to be filled or canceled 
	}
	
	@Override public synchronized void onStatusUpdated(
			DualOrder dualOrd, 
			int permId, 
			Action action, 
			int filled, 
			double avgPrice) {
		
		Util.wrap( () -> {

			// add or update order map if there was a fill; we don't care what state we are in
			// but we might give a warning if not in an expected state
			if (filled > 0) {
				updateOrderStatus( permId, action, filled, avgPrice);
			}
			
			Status status = status();

			// update sharesHeld which must be sent to Frontend
			double bought = adjust( totalBought() );
			put( "sharesHeld", bought - adjust( totalSold() ));
			put( "sharesToBuy", desiredQty() - bought);
			updateValueAndLoanAmt();
			
			out( "MarginOrder received status  id=%s  name=%s  status=%s  filled=%s/%s  avgPrice=%s", 
					permId, dualOrd.name(), status, filled, roundedQty(), avgPrice);
	
			if (status == Status.PlacedBuyOrder && dualOrd == m_entryOrder) {
				if (totalBought() >= roundedQty() ) {
					out( "  entry order has filled");
					
					status( Status.BuyOrderFilled);
					save();
					
					// make sure buy orders are both fully canceled
					cancelBuyOrders();
					
					placeSellOrders( null);
				}
			}
			else if (status == Status.PlacedSellOrders && (dualOrd == m_profitOrder || dualOrd == m_stopOrder) ) {
				if (totalSold() >= roundedQty() ) {
					out( "  sell orders have filled");
					
					status( Status.Completed);
					save();

					cancelSellOrders();
					
					// we should have zero position, so stop listening for stock updates
					prices().removeListener( m_listener);
					
					// we are done, nothing left to do; order will remain in Completed state
					// until user withdraws the cash
				}
			}
			else {
				out( "Error: received fill we were not expecting; maybe order was canceled?");
			}
		});
	}

	/** Place new sell orders OR sync up with the existing sell orders */
	void placeSellOrders(HashMap<String,LiveOrder> liveOrders) {
		try {
			placeSellOrders_( liveOrders);
		} catch (Exception e) {
			out( "Error placing sell orders; will try again in 30 sec");
			
			e.printStackTrace();
			
			// we have to keep trying
			Alerts.alert( "RefAPI", "COULD NOT PLACE MARGIN SELL ORDERS", 
					String.format( "orderId=%s  sharesHeld=%s  loanAmt=%s", orderId(), sharesHeld(), loanAmt() ) );
			
			Util.executeIn(30000, () -> placeSellOrders( liveOrders) );
		}
	}
	
	void placeSellOrders_(HashMap<String,LiveOrder> liveOrders) throws Exception  {
		Util.require( status() == Status.BuyOrderFilled, "Invalid status %s to place sell orders", status() );
		
		int quantity = roundedQty() - totalSold();
		
		if (profitTakerPrice() > 0) {
			placeProfitTaker( quantity, liveOrders);
		}

		// we must set our own stop loss price which is >= theirs
		if (stopLossPrice() > 0) {
			placeStopLoss( quantity, liveOrders);
		}
		
		status( Status.PlacedSellOrders);
		save();
		
		// waiting for sell orders to fill
	}			
	
	private void placeProfitTaker(int quantity, HashMap<String,LiveOrder> liveOrders) throws Exception {
		out( "***Placing margin profit-taker orders");
		
		m_profitOrder = new DualOrder( m_conn, null, "PROFIT", orderId() + " profit", this);
		m_profitOrder.action( Action.Sell);
		m_profitOrder.quantity( quantity);
		m_profitOrder.orderType( OrderType.LMT);
		m_profitOrder.lmtPrice( profitTakerPrice() );
		m_profitOrder.tif( TimeInForce.GTC);   // must consider this
		m_profitOrder.outsideRth( true);

		m_profitOrder.placeOrder( conid(), liveOrders);
	}

	private void placeStopLoss(int quantity, HashMap<String,LiveOrder> liveOrders) throws Exception {
		out( "***Placing margin stop-loss orders");
		
		// need to check m_filledPrimary; if < roundedQty, we need to adjust the size here. pas
		m_stopOrder = new DualOrder( 
				m_conn, 
				stock().prices(), 
				"STOP", 
				orderId() + " stop", 
				this);
		
		m_stopOrder.action( Action.Sell);
		m_stopOrder.quantity( quantity);
		m_stopOrder.orderType( OrderType.STP_LMT);  // use STOP_LMT  because STOP cannot be set to trigger outside RTH
		m_stopOrder.lmtPrice( stopLossPrice() * .95);
		m_stopOrder.stopPrice( stopLossPrice() );
		m_stopOrder.tif( TimeInForce.GTC);   // must consider this
		m_stopOrder.outsideRth( true);
		
		m_stopOrder.placeOrder( conid(), liveOrders);
	}
	
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
		m_store.save(); 
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
		case InitiatedPayment:
		case GotReceipt:
		case PlacedBuyOrder:
			cancelByUser();
			break;
			
		case BuyOrderFilled:
		case PlacedSellOrders:
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
		cancel( String.format( "Canceled by user; status was %s", status() ) );
	}
	
	private void cancel(String how) {
		put( "completedHow", how);
		
		cancelBuyOrders();
		
		// there must be no sell orders; we can't cancel if there are

		status( Status.Canceled);
		save();
		
		// if there's no position, we have no need for a listener
		if (totalBought() - totalSold() <= 0) {
			prices().removeListener(m_listener);
		}
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
	
	private void updateValueAndLoanAmt() {
		double cashValue = cashValue();
		put( "value", cashValue + stockValue() );
		put( "loanAmt",  Math.max( 0, cashValue() ) ); 
	}

	double value() {
		return stockValue() + cashValue();
	}
	
	double stockValue() {
		double stockPos = adjust( totalBought() ) - adjust( totalSold() );
		return stockPos * markPrice();
	}
	
	/** value is stock plus cash */
	double cashValue() {
		return 
			amtToSpend() + // the user may contribute more money later which we have to add in here  
			adjust( totalSold() ) * avgSellPrice() -
			adjust( totalBought() ) * avgBuyPrice() -
			fees();
			
	}

	// this should be IB commissions plus the 1% fee
	private double fees() {
		return .01 * amtToSpend() * leverage();
	}
	
	private double markPrice() {
		try {
			return prices().markPrice();
		} catch (Exception e) {
			out( "Error: no mark price for %s", stock().symbol() );
			return 0.;
		}
	}
	
	private int totalBought() {
		return totalBoughtOrSold( Action.Buy);
	}
	
	private int totalSold() {
		return totalBoughtOrSold( Action.Sell);
	}

	private int totalBoughtOrSold(Action action) {
		return Util.sumInt( orderMap().values(), item -> {
			JsonObject ord = (JsonObject)item;
			return ord.getString( "action").equals( action.toString() ) ? ord.getInt( "filled") : 0;
		});
	}
	
	/** here's the deal: if we have bought or sold the total rounded amount,
	 *  we use the desiredQty * avgPrice of all orders; otherwise, we use
	 *  the actual qty * avgPrice of all orders */
	private double avgBuyPrice() {
		return totalBoughtAmt() / totalBought();
	}
	
	private double avgSellPrice() {
		return totalSoldAmt() / totalSold();
	}
	
	private double totalBoughtAmt() {
		return totalAmount( Action.Buy);
	}
	
	private double totalSoldAmt() {
		return totalAmount( Action.Sell);
	}
	
	private double totalAmount( Action action) {
		return Util.sum( orderMap().values(), item -> {
			JsonObject ord = (JsonObject)item;
			return ord.getString( "action").equals( action.toString() ) 
					? ord.getInt( "filled") * ord.getDouble( "avgPrice")
					: 0;
		});
	}
	
	private void out( String format, Object... params) {
		S.out( orderId() + " " + format, params);
	}
	
	void status( Status status) {
		put( "status", status);
		// you could move "save()" here, it's called every time
	}

	/** maps permId to OrdStatus; 
	 *  saves the status of all IB orders that have been placed or filled
	 *  for this MarginOrder
	 *  
	 * it would be so much better if this could be a map of id->OrdStatus object
	 *  
	 * @throws Exception */  // create a JsonMap object, same as JsonObject but any type. pas
	
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

	private Stock stock() {
		return m_stocks.getStockByConid( conid() );
	}

	private Prices prices() {
		return stock().prices();
	}
	
	private void updateBidAsk(Prices prices) {
		put( "bidPrice", prices.bid() );
		put( "askPrice", prices.ask() );
	}

	// map orderId to order state; to try to cast this to OrderMap with types, maybe create a wrapper
	JsonObject orderMap() { 
		try {
			return getObject( "orderMap");
		} catch (Exception e) {
			e.printStackTrace();
			return null; // should never happen
		} 
	}  
	
	double sharesHeld() {
		return getDouble( "sharesHeld");
	}

	double loanAmt() {
		return getDouble( "loanAmt");
	}

	/** If there is a partial fill, return that amount; if we are 
	 *  completely filled, return the desired quantity (i.e. the decimal 
	 *  amount desired by the user) */ 
	private double adjust(int traded) {
		return traded >= roundedQty() ? desiredQty() : traded;
	}
}

//now
//you need a thread to retry in case there is a random moralis error
//auto-liq at COB regular hours if market is closed next day
//do liquidation
//do not accept order during extended hours if market is closed next day
//detect order being canceled
//handle failure when placing initial ib order
//handle system cancel event; replace order when appropriate
//let canceled orders remain for some time and then get cleared out; user must withdraw the cash
//don't allow user cancel if there is any bought size; just allow canceling remaining buy orders
//you need to cancel one when the other one fills; make sure stop is > liq order or place your own stop at the liq price
//allow modifying the order
//on reconnect, if status is PlacedBuyOrder, check that we have buy orders working
//synchronize access to status()
//allow user to add in more money later
//you have to remove the stock listener, but when? def. at least when the order is removed from the store
//now we need to cash out the user; they must initiate this
//don't allow place stop order above current price
//for now, if a working order is canceled by system, let it cancel the order here and move to next state
//don't allow cancel if there is any loan amount > 0, otherwise yes
//probably add the order status to the orderMap() so you can see the final status of all orders
//think about this: stop order triggers during day, night order is conv. to limit but does not fill, then gets canceled; you need to remember that it triggered  
//add the config items
//check for fill during reset, i.e. live savedOrder that is not restored
//test single stop order
//test dual stop orders
//test canceling at all different states

//later:
//implement user sell/liquidation, user modify stop/profit/entry prices
//build an index to return wallet order efficiently
//need an efficient way to save, can't write the whole store each time
//we need a thread that calls check() continuously; remove the 30 sec timer threads

//	old notes from textpad
//	
//	margin order
//	give a receipt
//	you have to transfer to a separate wallet because the stablecoin held in there is not yours
//	if the main order is partially filled, we won't place the bracket orders until it is fully filled or canceled
//	it's possible an order could fill and disappear while RefAPI is not running; you won't know if it filled; you will have to look for trade reports; or, maybe there is a way to download completed orders to refapi
//	Overnight sell stop TIF is DAY; you have to simulate GTC as well
//	since overnight must be day, what happens to the oca-connected Day order when the overnight order is canceled by the system; you may need to simulate oca group without the "cancel" connection
//	when placing the order, you must set a timer/timeout to respond for the blockchain 
//	don't store orders in the db; it's messing up the tags
//	test the night order trigger which should place a lmt order
//	if stocks are re-read, the "Prices" that we are listening to will be all wrong
//	you have to account for partial fills when creating the child orders
