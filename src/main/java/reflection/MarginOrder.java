package reflection;

import static reflection.Main.m_config;

import java.util.HashMap;
import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder;
import com.ib.client.DualOrder.DualParent;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import common.Alerts;
import common.Util;
import reflection.MarginTrans.GoodUntil;
import tw.util.S;
import web3.RetVal;
import web3.Stablecoin;
import web3.StockToken;

public class MarginOrder extends JsonObject implements DualParent {
	double feePct;
	double lastBuffer;  // as percent, try 
	double bidBuffer;
	double maxLeverage;
	double minUserAmt = 100;
	double maxUserAmt = 200;
	double maxLtv = 1.02;
	
	public enum Status {
		NeedPayment,			// initial state
		InitiatedPayment,		// initiated blockchain transaction; may or may not have trans hash and receipt
		GotReceipt,				// got blockchain hash and receipt
		PlacedBuyOrder,			// buy order was been placed; waiting for it to fill
		BuyOrderFilled,			// buy order has filled; need to place sell orders, if any 
		Monitoring,				// placed sell orders (if any); monitoring sell orders and for liquidation
		Liquidation,			// in liquidation
		Completed,				// we're done; nothing to monitor, no loan; there may be a position
		Canceled,				// canceled by user or system; nothing to monitor
		Withdrawing,			// withdrawing funds
		Settled;				// funds and/or shares have been withdrawn; the order can be removed from the screen

		private boolean is(Status... statuses) {
			for (var status : statuses) {
				if (this == status) {
					return true;
				}
			}
			return false;
		}

		/** for system cancel only */
		boolean canSystemCancel() {
			return is( NeedPayment, InitiatedPayment, GotReceipt, PlacedBuyOrder, Monitoring, BuyOrderFilled, Canceled);  
		}  
		
		boolean canUpdateEntryPrice() {
			return is( NeedPayment, InitiatedPayment, GotReceipt, PlacedBuyOrder);
		}

		boolean canUpdateBracketPrice() {
			return is( NeedPayment, InitiatedPayment, GotReceipt, PlacedBuyOrder, BuyOrderFilled, Monitoring);		
		}

		boolean couldHaveLiveOrder() {
			return is( PlacedBuyOrder, BuyOrderFilled, Monitoring, Liquidation);
		}

		boolean canWithdraw() {
			return is( Completed, Canceled, Settled);
		}		

	}

	String wallet() { return getString( "wallet_public_key"); }
	String orderId() { return getString( "orderId"); }
	int conid() { return getInt( "conid"); }
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
//	transHash
//	receipt
//	loanAmt, 
//	liquidationPrice, 
//	value (of position + cash)   
//	bidPrice,		// done 
//	askPrice, 		// done
//	sharesHeld,   // done
//	sharesToBuy,  // done
//	symbol,       // done
//  placed
	
	// transient, non-serializeable 
	private final ApiController m_conn;
	private final Stocks m_stocks;
	private final MarginStore m_store;
	private final Consumer<Prices> m_listener = prices -> onTickBidAsk( prices);
	boolean m_initiatedPayment; // set this flag if we have initiated payment in this session

	// the orders
	private DualOrder m_buyOrder;
	private DualOrder m_profitOrder;
	private DualOrder m_stopOrder;
	private DualOrder m_liqOrder;

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
		put( "desiredQty", totalSpend / entryPrice() );  // note that entryPrice cannot be zero
		put( "roundedQty", OrderTransaction.positionTracker.buyOrSell( conid(), true, desiredQty(), 1) );
		put( "sharesHeld", 0);
		put( "sharesToBuy", desiredQty() );
		put( "loanAmt", 0);
		put( "orderMap", new JsonObject() );
		put( "status", Status.NeedPayment);  // waiting for blockchain payment transaction
		put( "placed", System.currentTimeMillis() );

		postInit();
	}			

	/** Called when order is restore from MarginStore 
	 * @param store */
	MarginOrder( ApiController conn, Stocks stocks, MarginStore store) {
		m_conn = conn;
		m_stocks = stocks;
		m_store = store;
	}

	/** Called only once, after order is created or read from disk */
	public void postInit() {
		out( "postInit " + this);
		
		// NOTE: we don't set the TIF here; it is set in SingleOrder ctor
		
		m_buyOrder = new DualOrder( m_conn, null, "ENTRY", orderId() + " entry", conid(), this);
		m_buyOrder.action( Action.Buy);
		m_buyOrder.orderType( OrderType.LMT);
		m_buyOrder.lmtPrice( entryPrice() );
		m_buyOrder.outsideRth( true);

		if (profitTakerPrice() > 0) {
			m_profitOrder = new DualOrder( m_conn, null, "PROFIT", orderId() + " profit", conid(), this);
			m_profitOrder.action( Action.Sell);
			m_profitOrder.orderType( OrderType.LMT);
			m_profitOrder.lmtPrice( profitTakerPrice() );
			m_profitOrder.outsideRth( true);
		}
		
		if (stopLossPrice() > 0) {
			m_stopOrder = new DualOrder( m_conn, stock().prices(), "STOP", orderId() + " stop", conid(), this);
			m_stopOrder.action( Action.Sell);
			m_stopOrder.orderType( OrderType.STP_LMT);  // use STOP_LMT  because STOP cannot be set to trigger outside RTH
			m_stopOrder.lmtPrice( stopLossPrice() * .95);
			m_stopOrder.stopPrice( stopLossPrice() );
			m_stopOrder.outsideRth( true);
		}

		m_liqOrder = new DualOrder( m_conn, null, "LIQUIDATION", orderId() + " liquidation", conid(), this);
		m_liqOrder.action( Action.Sell);
		m_liqOrder.orderType( OrderType.LMT);
		m_liqOrder.outsideRth( true);
	}


	/** Called every time the connection is restored. Update the orders in the orderMap()
	 *  and set the Order on the SingleOrders (first time only) */ 
	public synchronized void onReconnected( HashMap<String,LiveOrder> orderRefMap) {
		// order is completed?
		if (status().is( Status.Completed, Status.Canceled, Status.Settled) ) {
			return;
		}
		
		out( "onReconnected margin order " + this);
		
		
		
		// fields of ordStatus are: permId, action, filled, avgPrice
		
		// update the saved live orders with the new set of live orders in case the status
		// or filled amount changed during the restart
		if (status().couldHaveLiveOrder() ) {
			for (var liveOrder : orderRefMap.values() ) {
				if (liveOrder.orderId().equals( orderId() ) ) {
					Util.wrap( () -> updateOrderStatus( 
							liveOrder.permId(), liveOrder.action(), liveOrder.filled(), liveOrder.avgPrice(), liveOrder.status() ) );
				}
			}
			
			m_buyOrder.rehydrate( orderRefMap);
			
			if (m_profitOrder != null) {
				m_profitOrder.rehydrate( orderRefMap);
			}
			
			if (m_profitOrder != null) {
				m_stopOrder.rehydrate( orderRefMap);
			}
		}

		// listen for mkt data updates
		if (status() == Status.Monitoring) {
			listen();
		}

		// you have to call onUpdated() in case order has filled more
	}
	
	/** called on a timer every few seconds; make sure the margin order is in sync with the IB order */ 
	// no good, we can't be calling this and methods like placeBuyOrder in different threads
	// you either have to only call them in this thread, or only call this after reconnect
	synchronized void onProcess() {
		switch( status() ) {
			case NeedPayment:
				acceptPayment();
				break;
				
			case InitiatedPayment:
				if (!m_initiatedPayment) {
					// this means we were waiting for payment and RefAPI restarted;
					// we don't know if we got it or not and we don't know how to
					// wait for it; we can improve this if we save and restore the info needed
					// to wait for a payment
					Alerts.alert(
							"RefAPI", 
							"MARGIN_ORDER PAYMENT IS LOST, HANDLE THIS", 
							S.format( "orderId=%s", orderId() ) );
					
					systemCancel( "Order payment was lost");
				}
				break;
				
			case GotReceipt:
				placeBuyOrder();
				break;
				
			case PlacedBuyOrder:
				// check that the buy order is still active; if not, it could
				// have been canceled or filled while disconnecting, or it could
				// be that placing the order failed and we need to try again
				// we have to find out what happened to buy order that
				// was open when the RefAPI reset; it was either filled or canceled
				checkBuyOrder();
				break;
			
			case BuyOrderFilled:
				placeSellOrders();
				break;

			case Monitoring:
				checkMonitoring();
				break;
			
			case Liquidation:
				checkLiquidation();
				break;
				
			case Canceled:
				m_buyOrder.checkCanceled();
				break;
				
			case Completed:
			case Settled:
				// nothing to do
				break;
		}
	}

	/** Executes in the MarginStore timer thread */
	public synchronized void acceptPayment() {
		// this can take a while; don't tie up the timer thread of the HTTP processing thread
		Util.execute( "AcceptPmt", () -> {
			try {
				acceptPayment_();
			}
			catch (Exception e) {
				e.printStackTrace();
				systemCancel( "Failed to accept payment - " + e.getMessage() );
			}
		});
	}
	
	/** Executes in a new thread every time. Cannot synchronize here
	 *  because it will block the processing thread.  */
	private void acceptPayment_() throws Exception  {
		require( status() == Status.NeedPayment, "Invalid status %s to accept payment", status() );
		
		out( "Accepting payment");
		
		// get current receipt balance
		String walletAddr = wallet();
		double amtToSpend = amtToSpend();
		StockToken receipt = m_stocks.getReceipt();
		double prevBalance = receipt.getPosition( walletAddr);  // or get from HookServer

		// wrong, don't pull config from Main, won't work for isolated testing outside RefAPI pas
		Stablecoin stablecoin = m_config.getStablecoin( currency() );

		// transfer the crypto to RefWallet and give the user a receipt; 
		// it would be good if we can find a way to tie the receipt to this order
		RetVal val = m_config.rusd().buyStock(walletAddr, stablecoin, amtToSpend, receipt, amtToSpend);
		
		// if order has been canceled by user while we were waiting, bail out
		synchronized( this) {
			if ( status() == Status.Canceled) {
				out( "Order was canceled while waiting for hash or receipt");
				return;
			}

			m_initiatedPayment = true;  // must come first; we need this because it will be a problem if process() is called and status is InitiatedPayment and this flag is not set 
			status( Status.InitiatedPayment);
		}

		// wait for transaction hash AND receipt; then check for canceled
		
		String hash = val.waitForHash();

		out( "Accepted payment with trans hash %s", hash);
		
		// update transaction hash on MarginOrder
		transHash( hash);
		save();
		
		// wait for receipt to register because a transaction hash is not a guarantee of success;
		// alternatively we could wait for some other type of blockchain finality
		int retVal = Util.waitFor( 60, () -> receipt.getPosition( 
				walletAddr) - prevBalance >= amtToSpend - .01);
		
		// NOTE: order could have been canceled while waiting, but even so, we should
		// have received the receipt
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
		synchronized( this) {
			if ( status() == Status.Canceled) {
				out( "Order was canceled while waiting for hash or receipt");
				return;
			}
			
			status( Status.GotReceipt);

			placeBuyOrder();
		}
		
		// NOTE: there is a window here. If RefAPI terminates before the blockchain transaction completes,
		// when it restarts we won't know for sure if the transaction was successful or not;
		// operator assistance would be required		
	}
	
	/** Called in either acceptPayment thread or MarginStore timer thread. Access is synchronized */
	private void placeBuyOrder() {
		try {
			placeBuyOrder_();
		}
		catch (Exception e) {
			out( e.getMessage() );
			e.printStackTrace();
		}
	}
	
	/** Called in either acceptPayment thread or MarginStore timer thread. Access is synchronized */
	private void placeBuyOrder_() throws Exception {
		
		require( status() == Status.GotReceipt, "Invalid status %s when placing buy order", status() );
		require( roundedQty() > 0, "Invalid quantities  roundedQty=%s  desiredQty=%s", roundedQty(), desiredQty() );
		
		out( "***Placing margin entry orders");

		// if we are restoring, and there is no live order, you have to subtract
		// out the filled amount and place for the remaining size only
		//int remainingQty = roundedQty() - totalBought();

		S.out( "modifying order margin=%s  dual=%s  day=%s  IB=%s", 
				hashCode(), 
				m_buyOrder.hashCode(), 
				m_buyOrder.m_dayOrder.hashCode(), 
				m_buyOrder.m_dayOrder.m_order.realhash() );

		
		m_buyOrder.quantity( roundedQty() );
		m_buyOrder.placeOrder( conid() );

		status( Status.PlacedBuyOrder);

		// now we wait for the buy order to be filled or canceled 
	}

	/** Calleded in the http processing thread */
	synchronized void onUpdated( double entryPrice, double profitTakerPrice, double stopPrice) throws Exception {
		if (entryPrice > 0 && Util.isNotEq( entryPrice, entryPrice(), .005) ) {
			
			Main.require( status().canUpdateEntryPrice(), RefCode.INVALID_REQUEST, "The entry price cannot be updated; the buy order has already been filled or canceled.");
			
			put( "entryPrice", entryPrice);
			m_buyOrder.lmtPrice( entryPrice() );
			
			// update the IB order if it is live
			if (status() == Status.PlacedBuyOrder) {
				m_buyOrder.resubmit();
			}
		}
		
		if (profitTakerPrice > 0 && Util.isNotEq( profitTakerPrice, profitTakerPrice(), .005) ) {
			Main.require( status().canUpdateBracketPrice(), RefCode.INVALID_REQUEST, "The profit-taker price cannot be updated at this time; the order status is '%s'", status() );

			double oldPrice = getDouble( "profitTakerPrice");
			
			put( "profitTakerPrice", profitTakerPrice);
			m_profitOrder.lmtPrice( profitTakerPrice);
			
			// update the IB order if it is live
			if (status() == Status.Monitoring) {
				m_profitOrder.resubmit();
			}
		}
		
		if (stopPrice > 0 && Util.isNotEq( stopPrice, stopLossPrice(), .005) ) {
			Main.require( status().canUpdateBracketPrice(), RefCode.INVALID_REQUEST, "The stop-loss price cannot be updated at this time; the order status is '%s'", status() );

			put( "stopLossPrice", stopPrice);
			m_stopOrder.stopPrice(stopPrice);
			m_stopOrder.lmtPrice(stopPrice * .95);

			// update the IB order if it is live
			if (status() == Status.Monitoring) {
				m_profitOrder.resubmit();
			}
		}
	}

	/** Buy orders should be working */
	private void checkBuyOrder() {
		int qtyToBuy = roundedQty() - totalBought();

		if (qtyToBuy > 0) { 
			m_buyOrder.checkOrder( qtyToBuy);
		}
		else {
			out( "WARNING: Buy order is completely filled but order is in state %s", status() );
			
			status( Status.BuyOrderFilled);

			cancelBuyOrders();
			
			placeSellOrders();
		}
	}

	/** check sell orders; no need to check for liquidation; 
	 *  that occurs in the onTick() method */
	private void checkMonitoring() {
		// sell orders should be working
		int qtyToSell = totalBought() - totalSold();
		
		if (qtyToSell > 0) {
			if (m_profitOrder != null) {
				m_profitOrder.checkOrder( qtyToSell);
			}
			
			if (m_stopOrder != null) {
				m_stopOrder.checkOrder( qtyToSell);
			}
		}
		// we could also be monitoring for liquidation 
	}

	private void checkLiquidation() {
		int qtyToSell = totalBought() - totalSold();
		
		if (qtyToSell > 0) {
			m_liqOrder.checkOrder( qtyToSell);
		}
		else {
			out( "WARNING: Sell orders are filled but order is in state %s", status() );
		}
	}
	
	/** Completed orders should not have any size or orders */
//	private void checkCompleted() {
//	}

	/** Called when an order is updated; note that it is called in the API processing thread;
	 *  we could put it in the margin thread if desired */
	@Override public synchronized void onStatusUpdated(
			DualOrder dualOrd,
			OrderStatus ibOrderStatus,
			int permId, 
			Action action, 
			int filled, 
			double avgPrice) {
		
		Util.wrap( () -> {

			// add or update order map if there was a fill; we don't care what state we are in
			// but we might give a warning if not in an expected state
			if (filled > 0 || ibOrderStatus != OrderStatus.Cancelled) {
				updateOrderStatus( permId, action, filled, avgPrice, ibOrderStatus);  // we could add OrderStatus here if desired. pas
			}
			else {
				orderMap().remove( "" + permId);
			}
			
			final Status status = status();

			// update attributes which must be sent to Frontend
			double bought = adjust( totalBought() );
			put( "sharesHeld", bought - adjust( totalSold() ));
			put( "sharesToBuy", desiredQty() - bought);

			double cashBalance = cashBalance();
			put( "value", cashBalance + stockValueLast() );
			put( "loanAmt",  cashBalance < 0. ? -cashBalance : 0.);

			S.out( "-----> shares=%s  needed=%s  cash=%s  val=%s  loan=%s", 
					sharesHeld(), sharesToBuy(), cashBalance, value(), loanAmt() );
			S.out( this);
			
			out( "MarginOrder received status  id=%s  name=%s  status=%s  filled=%s/%s  avgPrice=%s", 
					permId, dualOrd.name(), status, filled, roundedQty(), avgPrice);
	
			if (status == Status.PlacedBuyOrder && dualOrd == m_buyOrder) {
				if (totalBought() >= roundedQty() ) {
					out( "  entry order has filled");
					
					status( Status.BuyOrderFilled);
					
					// make sure buy orders are both fully canceled
					cancelBuyOrders();
					
					placeSellOrders();
				}
			}
			else if (status == Status.Monitoring && (dualOrd == m_profitOrder || dualOrd == m_stopOrder) ) {
				if (totalSold() >= totalBought() ) {
					out( "  sell orders have filled");
					
					status( Status.Completed);

					cancelSellOrders();
					
					// we should have zero position, so stop listening for stock updates
					prices().removeListener( m_listener);
					
					// we are done, nothing left to do; order will remain in Completed state
					// until user withdraws the cash
				}
			}
			else if (status == Status.Liquidation && dualOrd == m_liqOrder) {
				if (totalSold() >= totalBought() ) {
					out( "  liquidation has completed");

					status( Status.Completed);
					
					cancelLiqOrder();
					
					// we are dont
				}
			}
//			else {
//				probably just one of the extra day/night orders being canceled; we can ignore it
//			}
		});
	}

	/** Place new sell orders. Called in API or margin thread */
	private synchronized void placeSellOrders() {
		try {
			placeSellOrders_();
		}
		catch (Exception e) {
			out( "Error placing sell orders; will try again in 30 sec");
			
			e.printStackTrace();
			
			// we have to keep trying
			Alerts.alert( "RefAPI", "COULD NOT PLACE MARGIN SELL ORDERS", 
					String.format( "orderId=%s  sharesHeld=%s  loanAmt=%s", orderId(), sharesHeld(), loanAmt() ) );
		}
	}
	
	private void placeSellOrders_() throws Exception  {
		require( status() == Status.BuyOrderFilled, "Invalid status %s to place sell orders", status() );
		
		boolean someSell = false;
		int quantity = roundedQty() - totalSold();
		
		if (profitTakerPrice() > 0) {
			out( "***Placing margin profit-taker orders");
			m_profitOrder.quantity( quantity);
			m_profitOrder.placeOrder( conid() );
			someSell = true;
		}

		// we must set our own stop loss price which is >= theirs
		if (stopLossPrice() > 0) {
			out( "***Placing margin stop-loss orders");
			m_stopOrder.quantity( quantity);
			m_stopOrder.placeOrder( conid() );
			someSell = true;
		}
		
		if (someSell || loanAmt() > 0) {  // waiting for sell orders to fill or monitoring for liquidation
			status( Status.Monitoring);
			listen();
		}
		else {  // we're done
			status( Status.Completed);
		}
	}			
	
	/** called in API or margin thread */
	private void cancelBuyOrders() {
		out( "canceling buy orders");
		m_buyOrder.cancel();
	}

	/** called in API or margin thread */
	private void cancelSellOrders() {
		if (m_profitOrder != null) {
			out( "canceling profit order");
			m_profitOrder.cancel();
		}
		
		if (m_stopOrder != null) {
			out( "canceling stop orders");
			m_stopOrder.cancel();
		}
	}
	
	/** Called in API thread */
	private void cancelLiqOrder() {
		out( "canceling liquidation order");
		m_liqOrder.cancel();
	}

	/** save entire order store in 500 ms */
	private void save() {
		m_store.saveLater(); 
	}

	private void transHash(String hash) {
		put( "transHash", hash);
	}

	private void gotReceipt(boolean v) {
		put( "gotReceipt", v);
	}

	/** called by user in HTTP processing thread */
	public synchronized void userCancel() throws RefException {
		out( "User cancel");
		
		switch( status() ) {
		case NeedPayment:
		case InitiatedPayment:
		case GotReceipt:
		case PlacedBuyOrder:
		case BuyOrderFilled:
		case Monitoring:
			cancelBuyOrders();  // buy orders can always be canceled
			cancelSellOrders();

			require( loanAmt() <= 0, RefCode.CANT_CANCEL, "The buy and sell orders, if any, have been canceled. The order has a positive loan amount and will remain active.");
			
			status( Status.Canceled);
			put( "completedHow", "Canceled by user");
			prices().removeListener(m_listener);
			
			break;

		case Liquidation:
			require( false, RefCode.CANT_CANCEL, "It's too late to cancel; the position is being liquidated");
			break;
			
		case Completed:
		case Settled:
			require( false, RefCode.CANT_CANCEL, "The order has already completed");
			break;
			
		case Canceled:
			require( false, RefCode.CANT_CANCEL, "The order has already been canceled");
			break;
		}
	}

	/** Called internally and by Monitor */
	public synchronized void systemCancel(String how) {
		if ( !status().canSystemCancel() ) {
			out( "Can't cancel order with status %s", status() );
		}
		else {
			out( "System-canceling order - %s", how);
		
			cancelBuyOrders();  // buy orders can always be canceled
			cancelSellOrders();
	
			if (loanAmt() > 0) {
				out( "WARNING: order with loan amount was system-canceled");
			}

			// set status first so if it fails, we will come back here
			status( Status.Canceled);
			put( "completedHow", how);
			prices().removeListener(m_listener);
		}
	}

	public synchronized void withdrawFunds() throws Exception {
		require( status().canWithdraw(), RefCode.INVALID_REQUEST, "Funds cannot be withdrawn at this time");
		
		double stockPos = netAdjusted();
		require( stockPos == 0, RefCode.INVALID_REQUEST, "Please liquidate your position before withdrawing the cash");

		double cashBalance = cashBalance();
		require( cashBalance > 0, RefCode.INVALID_REQUEST, "There is no cash balance to withdraw");

		// check that the wallet is still holding the receipt
		StockToken receipt = m_stocks.getReceipt();
		require( Util.isEq( receipt.getPosition( wallet() ), amtToSpend(), .01),
				RefCode.NO_RECEIPT, 
				"The cash cannot be withdrawn because the wallet is no longer holding the receipt; please contact support");
		
		// initially, we took the users stablecoin and we minted amtToSpend Receipt into their wallet
		// now we want to take out the receipt and put in money and/or more stock
		Util.execute( () -> {
			try {
				m_config.rusd().sellStockForRusd( wallet(), cashBalance, receipt, amtToSpend() );
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/** for liquidation calculation purposes, we have to use mark price which uses the bid
	 *  price; for display purposes, we use the last price because we will frequently have
	 *  a last price even if there is no bid */
	private double stockValueMark() throws Exception {
		double stockPos = netAdjusted();
		return stockPos * prices().markPrice();
	}
	
	private double stockValueLast() {
		double stockPos = netAdjusted();
		return stockPos * prices().last();
	}
	
	/** This current stock position, from the user's perspective */
	private double netAdjusted() {
		return adjust( totalBought() ) - adjust( totalSold() );
	}
	
	/** value is stock plus cash */
	private double cashBalance() {
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
		return div( totalBoughtAmt(), totalBought() );
	}
	
	private double avgSellPrice() {
		return div( totalSoldAmt(), totalSold() );
	}
	
	/** divide and avoid NaN if bot is zero */
	private double div(double top, double bot) {
		return bot != 0 ? top / bot : 0;
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
	
	/** Update status and start or stop listening */
	private void status( Status status) {
		if (status == Status.Monitoring) {
			stopListening();
		}

		Object prev = put( "status", status);
		out( "Set status  %s -> %s", prev, status); 
		save();
		
		if (status == Status.Monitoring) {
			listen();
		}
	}

	/** maps permId to OrdStatus; 
	 *  saves the status of all IB orders that have been placed or filled
	 *  for this MarginOrder
	 *  
	 * it would be so much better if this could be a map of id->OrdStatus object
	 *  
	 * @throws Exception */  // create a JsonMap object, same as JsonObject but any type. pas
	
	private void updateOrderStatus( int permId, Action action, int filled, double avgPrice, OrderStatus status) throws Exception {
		getOrCreateOrderStatus( permId, action)
			.putAll( Util.toJson( "filled", filled, "avgPrice", avgPrice, "status", status) );  // we could add orderStatus here if needed
	}
	
	/** Return a value for the IB order map; it's a json with permId, action, status, filled, and avgPrice */
	private JsonObject getOrCreateOrderStatus( int permId, Action action) throws Exception {
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
	
	/** update bid/ask fields which are displayed by Frontend; called
	 *  in the Util.m_timer thread 
	 *  
	 *  Alternatively, we could NOT receive these ticks and just check
	 *  for liquidation in the process() method which gets called every
	 *  n seconds anyway */
	private synchronized void onTickBidAsk(Prices prices) {
		put( "bidPrice", prices.bid() );
		put( "askPrice", prices.ask() );
		
		put( "value", cashBalance() + stockValueLast() );
		
		if (status() == Status.Monitoring) {
		//if (status() != Status.PlacedBuyOrder || status() == Status.BuyOrderFilled) {
			double loanVal = loanAmt();
			
			if (loanVal > 0) {     // loan value and liq. is wrong; 
				try {
					double stockVal = stockValueMark();
					if (stockVal < loanVal * maxLtv) {
						out( "LIQUIDATING! Stock value of %s cannot support loan amount of %s", stockVal, loanVal);
						liquidate();
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
					Alerts.alert( "MarginMgr", "WARNING: Cannot calculate stock value for margin order",
							String.format( "wallet=%s  orderId=%s", wallet(), orderId() ) );
				}
			}
		}
	}

	/** Called when the price ticks and the stock value has dropped below min required value.
	 *  Note we don't care if there is a loan or not, we are going to sell at the bid. 
	 * @throws Exception */
	protected void userLiquidate() throws Exception {
		require( status() != Status.Liquidation, RefCode.INVALID_REQUEST, "The order is already in liquidation");
		
		cancelSellOrders();
		
		double net = totalBought() - totalSold();

		require( net > 0, RefCode.INVALID_REQUEST, "There is no position to liquidate; any resting orders will be canceled");

		liquidate();
	}

	private synchronized void liquidate() {
		status( Status.Liquidation);
		
		out( "***Liquidating");

		cancelSellOrders();

		int qty = OrderTransaction.positionTracker.buyOrSell( 
				conid(), false, totalBought() - totalSold(), 1);
		
		try {
			m_liqOrder.quantity( qty);
			m_liqOrder.lmtPrice( prices().bid() * .98);
			m_liqOrder.placeOrder( conid() );
		} catch (Exception e) {
			e.printStackTrace();
			
			Alerts.alert( "RefAPI", "COULD NOT PLACE MARGIN LIQUIDATION ORDER", 
					String.format( "orderId=%s  sharesHeld=%s  loanAmt=%s", orderId(), sharesHeld(), loanAmt() ) );
		}
	}
	/** map IB order permId (as string) to order state; to try to cast this to OrderMap with types, maybe create a wrapper;
	    the map contains all live orders and all filled orders; unfilled canceled orders are removed */
	private JsonObject orderMap() { 
		try {
			return getObject( "orderMap");
		} catch (Exception e) {
			e.printStackTrace();
			return null; // should never happen
		} 
	}  
	
	private double sharesHeld() {
		return getDouble( "sharesHeld");
	}

	private double sharesToBuy() {
		return getDouble( "sharesToBuy");
	}

	private double loanAmt() {
		return getDouble( "loanAmt");
	}

	private double value() {
		return getDouble( "value");
	}

	/** If there is a partial fill, return that amount; if we are 
	 *  completely filled, return the desired quantity (i.e. the decimal 
	 *  amount desired by the user) */ 
	private double adjust(int traded) {
		return traded >= roundedQty() ? desiredQty() : traded;
	}

	private void listen() {
		out( "Listening for mkt data updates");
		prices().addListener( m_listener);
	}

	private void stopListening() {
		out( "Stop listening for mkt data updates");
		prices().removeListener( m_listener);
	}

	public void require(boolean test, RefCode refCode, String text, Object... params) throws RefException {
		Main.require( test, refCode, orderId() + " " + text, params);
	}

	public void require(boolean test, String text, Object... params) throws Exception {
		Util.require( test, orderId() + " " + text, params);
	}

}

//auto-liq at COB regular hours if market is closed next day
//check for fill during reset, i.e. live savedOrder that is not restored; query completed orders
//let orders be pruned after one week (configurable)
//implement withdraw
//entry price higher is okay, you just need to adjust down the order quantities, and check status
//you could charge a different fee for a lev order with leverage of 1, something more similar to the 
//consider if you really want to collect a fee for an order that is never filled
//implement "get info" from frontend--or--send an email with the summary
//support user-liquidate, maybe force that
//purge the old margin orders
//you will run into issues with multiple users, you cannot have crossing orders for the same stock; cross them at the current mark price (bid/last/ask)
//must implement gooduntil and liquidation
//test withdraw cash and all possible errors
//test single stop order, sim. stop orders
//test dual stop orders
//add a pnl field
//just have one "cash/loan balance" column; you don't need separate columns

//later:
//need pagination at frontend, 
//check, will filled or canceled orders ever be downloaded in the liveorders? test and consider that
//now we need to cash out the user; they must initiate this
//remove the margin order stock listener, when the order is removed from the store
//allow modifying the order
//let canceled orders remain for some time and then get cleared out; user must withdraw the cash
//implement user sell/liquidation, user modify stop/profit/entry prices
//build an index to return wallet order efficiently
//need an efficient way to save, can't write the whole store each time
//we need a thread that calls check() continuously; remove the 30 sec timer threads
//allow user to add in more money later
//update Monitor to show more info per wallet, e.g. margin orders, live orders, etc
//move the marginstore into the database using a json field for the entire order?
//suppor increasing the buy price or increasing the buyAmount
//support time_t values for good until, it will be good for testing
//add orders from here to the UserTok mgr?
//very concerning bug: transaction was successful but never got "receipt" 0xd2c5d0cf7086832e89f035d066c3db04d1f8c6d035390b75db747e208defb621
//you have to protect against file corruption because the maps could be modified while writing; maybe move the existing file away before writing the new one, and alway try to read it if the main one can't be read
//review how you are waiting for blockchain transactions, e.g. acceptPayment
//add some detection for an order that is continuously placed and canceled by IB
//support withdrawing stock and money
//you have to think: does 0 price on a mod mean remove it, or ignore it; don't allow zero after orig has price
//price check on updates has been disabled; reconsider it

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
