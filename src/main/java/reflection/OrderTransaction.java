package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.Vector;

import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.StockToken;
import tw.util.S;
import util.LogType;

public class OrderTransaction extends MyTransaction {
	enum LiveOrderStatus { Working, Filled, Failed };

	private Order m_order;
	private double m_desiredQuantity;  // same as Order.m_totalQuantity. is that true? remove one. pas
	private Stock m_stock;
	private double m_stablecoinAmt;
	private double m_tds;
	private boolean m_respondedToOrder;
	private String m_walletAddr;
	
	
	// live order fields
	private String m_description = "";  // either order description or error description
	private LiveOrderStatus m_status = LiveOrderStatus.Working;
	private int m_progress = 10;
	private RefCode m_errorCode;
	
	public OrderTransaction(Main main, HttpExchange exch) {
		super(main, exch);
 	}
	
	String walletAddr() {
		return m_walletAddr;
	}
	
	/** Msg received directly from Frontend via nginx */
	public void backendOrder() {
		// any problem in here calls respond()
		wrap( () -> {
			require( "POST".equals(m_exchange.getRequestMethod() ), RefCode.INVALID_REQUEST, "order and check-order must be POST"); 
			parseMsg();
			order();
		});
    }
	
	// you have to make sure that the timeout doesn't happen and respond 0 while we are waiting
	// for the 

	private void order() throws Exception {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
		m_stock = m_main.getStock(conid);  // throws exception if conid is invalid

		String side = action();
		require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");

		m_desiredQuantity = m_map.getRequiredDouble( "quantity");
		require( m_desiredQuantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");

		double price = m_map.getRequiredDouble( "tokenPrice");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double preCommAmt = price * m_desiredQuantity;
		double maxAmt = side == "buy" ? m_config.maxBuyAmt() : m_config.maxSellAmt();
		require( preCommAmt <= maxAmt, RefCode.ORDER_TOO_LARGE, "The total amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( maxAmt) ); // this is displayed to user
		
		m_walletAddr = m_map.getRequiredParam("wallet_public_key");
		require( Util.isValidAddress(m_walletAddr), RefCode.INVALID_REQUEST, "Wallet address is invalid");
		
		// make sure user is signed in with SIWE and session is not expired
		// only trade and redeem messages need this
		validateCookie(m_walletAddr);
		
		// calculate order price
		double prePrice = side == "buy" 
			? price - price * m_config.minBuySpread()
			: price + price * m_config.minSellSpread();
		double orderPrice = Util.round( prePrice);  // round to two decimals
		
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		m_order = new Order();
		m_order.action( side == "buy" ? Action.BUY : Action.SELL);
		m_order.totalQuantity( m_desiredQuantity);
		m_order.lmtPrice( orderPrice);
		m_order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
		m_order.allOrNone(true);  // all or none, we don't want partial fills
		m_order.transmit( true);
		m_order.outsideRth( true);

		// check TDS calculation
		m_tds = m_map.getDouble("tds");
		double myTds = m_order.isBuy() ? 0 : (preCommAmt - m_config.commission() ) * .01;
		// fix this -> require( Util.isEq( m_tds, myTds, .001), RefCode.INVALID_REQUEST, "TDS of %s does not match calculated amount of %s", m_tds, myTds); 
		
		m_stablecoinAmt = m_map.getDouble("amount");
		if (m_stablecoinAmt == 0) {
			m_stablecoinAmt = m_map.getDouble("price");  // remove this after frontend is upgraded and change above to "getrequireddouble()"
		}
		
		double myStablecoinAmt = m_order.isBuy()
			? preCommAmt + m_config.commission()
			: preCommAmt - m_config.commission() - m_tds;
		// fix this-> require( Util.isEq(myStablecoinAmt, m_stablecoinAmt, .001), RefCode.INVALID_REQUEST, "The total order amount of %s does not match the calulated amount of %s", m_stablecoinAmt, myStablecoinAmt);
		
		// confirm that the user has enough stablecoin or stock token in their wallet
		// fix this-> requireSufficientStablecoin(order);		
		
		// check trading hours
		require( 
				m_main.m_tradingHours.insideAnyHours( 
						m_stock.getBool("is24hour"), 
						m_map.get("simtime"), 
						() -> contract.exchange("IBEOS") ),  // this executes only if SMART is closed but IBEOS is open 
				RefCode.EXCHANGE_CLOSED, 
				exchangeIsClosed);
		
		// check the dates (applies to stock splits only)
		m_main.m_tradingHours.checkSplitDates( m_map.get("simtime"), m_stock.getStartDate(), m_stock.getEndDate() );
		
		// check that we have prices and that they are within bounds;
		// do this after checking trading hours because that would
		// explain why there are no prices which should never happen otherwise
		Prices prices = m_main.getStock(conid).prices();
		prices.checkOrderPrice( m_order, orderPrice, m_config);
		
		// ***check that the prices are pretty recent; if they are stale, and order is < .5, we will fill the order with a bad price. pas
		// * or check that ANY price is pretty recent, to we know prices are updating
		
		log( LogType.REC_ORDER, m_order.getOrderLog(contract, m_walletAddr) );
				
		respond( code, RefCode.OK, "id", m_uid);
		
		// now it is up to the live order system to report success or failure
		walletLiveOrders().add( this);

		shrinkWrap( () -> {
			// *if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas
			if (m_order.roundedQty() == 0) {
				m_order.status(OrderStatus.Filled);
				onIBOrderCompleted(0, false);
			}
			// AUTO-FILL - for testing only
			else if (m_config.autoFill() ) {
				require( !m_config.isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
				log( LogType.AUTO_FILL, "id=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
						m_order.orderId(), m_order.action(), m_order.totalQty(), m_order.totalQty(), m_order.lmtPrice(),
						m_config.commission(), 0, "");
				m_order.status(OrderStatus.Filled);
				onIBOrderCompleted( m_order.totalQuantity(), false ); // you might want to sometimes pass false here when testing
			}
			else {
				submitOrder(  contract);
			}
		});
	}

	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require() and no wrap(),
	 *  only shrinkWrap()  */
	private void submitOrder( Contract contract) throws Exception {
		ModifiableDecimal shares = new ModifiableDecimal();

		// place order for rounded quantity
		m_main.orderController().placeOrModifyOrder(contract, m_order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

				shrinkWrap( () -> {
					m_order.status(status);
					out( "  order status  id=%s  status=%s", m_order.orderId(), status);

					// save the number of shares filled
					shares.value( filled.toDouble() );
					//shares.value = filled.toDouble() - 1;  // to test partial fills

					// better is: if canceled w/ no shares filled, let it go to handle() below

					if (status.isComplete() ) {
						m_order.permId(permId);
						onIBOrderCompleted( shares.value(), false);
					}
				});
			}

			@Override public void handle(int errorCode, String errorMsg) {
				shrinkWrap( () -> {
					log( LogType.ORDER_ERR, "id=%s  errorCode=%s  errorMsg=%s", m_order.orderId(), errorCode, errorMsg);

					// if some shares were filled, let orderStatus or timeout handle it
					if (shares.nonZero() ) {
						return;
					}

					// no shares filled

					// price does not conform to market rule, e.g. too many decimals
					if (errorCode == 110) {
						throw new RefException( RefCode.INVALID_PRICE, errorMsg);
					}

					// order rejected, never sent to exchange; could be not enough liquidity
					if (errorCode == 201) {
						throw new RefException( RefCode.REJECTED, errorMsg);
					}
				});
			}
		});

		log( LogType.SUBMITTED, m_order.getOrderLog(contract, m_walletAddr) );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( m_config.orderTimeout(), () -> onOrderTimeout( shares.value() ) );
	}
	
	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require()
	 *  this is wrapped() but it must call onIBOrderCompleted() so cannot throw an exception  */
	private synchronized void onOrderTimeout(double filledShares) {
		shrinkWrap( () -> {
			if (!m_respondedToOrder) {
				// this will happen if our timeout is lower than the timeout of the IOC order
				log( LogType.ORDER_TIMEOUT, "id=%s   order timed out with %s shares filled and status %s", 
						m_order.orderId(), filledShares, m_order.status() );

				// if order is still live, cancel the order; don't let an error here disrupt processing
				if (!m_order.status().isComplete() && !m_order.status().isCanceled() ) {
					try {
						out( "Canceling order %s on timeout", m_order.orderId() );
						m_main.orderController().cancelOrder( m_order.orderId(), "", null);
					}
					catch( Exception e) {
						e.printStackTrace();
					}
				}
				onIBOrderCompleted( filledShares, true);
			}
		});
	}

	/** We cannot throw an exception. We have already called respond(), we must now be sure to update the live order */
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  This call is shrinkWrapped()
	 *  In the case where order qty < .5 and we didn't submit an order, orderStatus will be Filled.
	 *  You must (a) make a log entry and (b) either call m_liveOrder.filled() or throw an exception */ 
	private synchronized void onIBOrderCompleted(double filledShares, boolean timeout) throws Exception {
		if (m_respondedToOrder) {
			return;    // should never happen, but just to be safe
		}
		m_respondedToOrder = true;

		require(
				filledShares > 0 || m_order.status() == OrderStatus.Filled,
				timeout ? RefCode.TIMED_OUT : RefCode.UNKNOWN,
				timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.");
		

		// ----- the order has been filled or partially filled -----

		
		double stockTokenQty;  // quantity of stock tokens to swap
		LogType logType;  // fill or partial fill

		// for a filled order, the (order size - filled size) should always be <= .5
		// if > .5, then it was a partial fill and we will use the filled size instead of the order size
		// this way we always have max .5 shares difference between stock pos and token pos (for a single order)
		if (m_order.totalQuantity() - filledShares > .5001) {
			stockTokenQty = filledShares;
			logType = LogType.PARTIAL_FILL;  // this should never happen since we set all-or-none on the orders
		}
		else {
			stockTokenQty = m_order.totalQuantity();
			logType = LogType.FILLED;
		}
		
		log( logType, "orderId=%s  filledShares=%s", m_order.orderId(), filledShares);
		
		if (fireblocks() ) {
			onUpdateStatus(FireblocksStatus.STOCK_ORDER_FILLED);
			processFireblocks(stockTokenQty, filledShares);
		}
		else {
			onFilled();
		}
	}
	
	/** Call to this method is shrinkWrapped() */
	private void processFireblocks(double stockTokenQty, double filledShares) throws Exception {
		try {			
			// for testing
			if (m_map.getBool("fail") ) {
				throw new Exception("Blockchain transaction failed intentially during testing"); 
			}

			out( "Starting Fireblocks protocol");

			String id;

			// buy
			if (m_order.action() == Action.BUY) {
				
				// buy with RUSD?
				if (m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.RUSD) {
					id = m_config.rusd().buyStockWithRusd(
							m_walletAddr, 
							m_stablecoinAmt,
							newStockToken(),
							stockTokenQty
					);
				}
				
				// buy with BUSD
				else {
					id = m_config.rusd().buyStock(
							m_walletAddr,
							m_config.busd(),
							m_stablecoinAmt,
							newStockToken(), 
							stockTokenQty
					);
				}
			}
			
			// sell
			else {
				id = m_config.rusd().sellStockForRusd(
						m_walletAddr,
						m_stablecoinAmt,
						newStockToken(),
						stockTokenQty
				);
			}
			
			// the FB transaction has been submitted; there is a little window here where an
			// update from FB could come and we would miss it because we have not added the
			// id to the map yet; we could fix this with synchronization
			insertToCryptoTable(id);
			
			allLiveOrders.put(id, this);
			
			// if we don't make it to here, it means there was an exception which will be picked up
			// by shrinkWrap() and the live order will be failed()
		}
		catch( Exception e) {  // for FB errors, we don't need to print a stack trace; maybe throw RefException for those
			if (!(e instanceof RefException) ) {
				e.printStackTrace();
			}
			
			log( LogType.ERROR, "orderId=%s  %s", m_order.orderId(), e.getMessage() ); 

			// unwind the order first and foremost
			unwindOrder(filledShares);
			
			// now throw an exception so the LiveOrder will get updated with the error text

			// fireblocks has failed; try to determine why
			
			// confirm that the user has enough stablecoin or stock token in their wallet
			requireSufficientStablecoin(); // check this again; it could have changes since the order was placed

			// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
			if (m_order.isBuy() && m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC) {
				double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() ); 
				require( 
						Util.isGtEq(approvedAmt, m_stablecoinAmt), 
						RefCode.INSUFFICIENT_ALLOWANCE,
						"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, m_stablecoinAmt); 
			}

			// we don't know why it failed, so throw the Fireblocks error
			throw e;
		}
	}

	private void insertToCryptoTable(String id) {
		try {
			JsonObject obj = new JsonObject();
			obj.put("orderid", m_order.orderId() );  // ties the order to the trades
			obj.put("permid", m_order.permId() );    // have to make sure this is set. pas
			obj.put("fireblocks_id", id);
			obj.put("timestamp", System.currentTimeMillis() / 1000);
			obj.put("wallet_public_key", m_walletAddr);
			obj.put("symbol", m_stock.getSymbol() );
			obj.put("conid", m_stock.getConid() );
			obj.put("action", m_order.action().toString() );
			obj.put("quantity", m_desiredQuantity);
			obj.put("price", m_order.lmtPrice() );
			obj.put("commission", m_config.commission() ); // not so good, we should get it from the order. pas
			obj.put("spread", 0); // really want the average filled price here
			obj.put("tds", m_tds);  // format this? pas
			obj.put("currency", m_map.getEnumParam("currency", Stablecoin.values() ).toString() );
			//"status"
			//"blockchain_hash"
			//"status"
			//"ip_address"
			//"city"
			//"country"
			// "crypto_id"
		
			m_main.sqlConnection( conn -> conn.insert("crypto_transactions", obj, "fireblocks_id = '%s'", id) );
		} 
		catch (Exception e) {
			log( LogType.ERROR, "Error inserting record into crypto_transactions table: " + e.getMessage() );
			e.printStackTrace();
		}
	}

	/** Confirm that the user has enough stablecoin or stock token in their wallet.
	 *  This could be called before or after submitting the stock order */
	private void requireSufficientStablecoin() throws Exception {
		if (m_order.isBuy() ) {
			double balance = stablecoin().getPosition( m_walletAddr );
			require( Util.isGtEq(balance, m_stablecoinAmt ), 
					RefCode.INSUFFICIENT_FUNDS,
					"The stablecoin balance (%s) is less than the total order amount (%s)", 
					balance, m_stablecoinAmt );
		}
		else {
			double balance = newStockToken().getPosition( m_walletAddr );
			require( Util.isGtEq(balance, m_desiredQuantity), 
					RefCode.INSUFFICIENT_FUNDS,
					"The stock token balance (%s) is less than the order quantity (%s)", 
					balance, m_desiredQuantity);
		}
	}

	private StockToken newStockToken() {
		return new StockToken( m_stock.getSmartContractId() );
	}

	/** The order was filled, but the blockchain transaction failed, so we must unwind the order. 
	 * @param filledShares */
	private void unwindOrder(double filledShares) {
		try {
			// don't unwind order in auto-fill mode which is for testing only
			if (m_config.autoFill() ) {
				out( "Not unwinding order in auto-fill mode");
				return;
			}

			String body = String.format( "The blockchain transaction failed and the order should be unwound:  wallet=%s  orderid=%s",
					m_walletAddr, m_order.orderId() );
			alert( "UNWIND ORDER", body);
			
			Contract contract = new Contract();
			contract.conid( m_map.getRequiredInt("conid") );
			contract.exchange( m_main.getExchange( contract.conid() ) );
			
			m_order.flipSide();
			m_order.orderId(0);
			m_order.orderType(OrderType.MKT);
			
			// this should never be the case since the orders are AON, but just in case that changes...
			if (filledShares < m_order.totalQuantity() ) {
				out( "WARNING: filled shared was less that total order qty when unwinding order"); 
				m_order.totalQuantity(filledShares);
			}
			
			m_main.orderController().placeOrModifyOrder(contract, m_order, null);
		}
		catch( Exception e) {
			e.printStackTrace();
			alert( "Error occurred while unwinding order", e.getMessage() );
		}
	}

	private boolean fireblocks() throws RefException {
		return m_config.useFireblocks() && !m_map.getBool("noFireblocks");
	}

	String action() throws RefException { 
		return m_map.getRequiredParam("action"); 
	}

	private Vector<OrderTransaction> walletLiveOrders() {
		return Util.getOrCreate(liveOrders, m_walletAddr.toLowerCase(), () -> new Vector<OrderTransaction>() );
	}

	/** Like wrap, but instead of notifying the http client, we update the live order */
	private void shrinkWrap(ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			out( e);
			log( LogType.ERROR, e.toString() );
			onFail(e); // you have to sync this. pas
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			onFail(e); // you have to sync this. pas
		}
	}
	
	boolean isBuy() {
		return m_order.isBuy();
	}

	public void onBlockchainOrderFailed() throws Exception {
		log( LogType.BLOCKCHAIN_FAILED, "The blockchain order failed.  Approved=%s  BUSD=%s  RUSD=%S  StockToken=%s",
				Main.m_config.busd().getAllowance( m_walletAddr, Main.m_config.rusdAddr() ),
				Main.m_config.busd().getPosition(m_walletAddr),
				Main.m_config.rusd().getPosition(m_walletAddr),
				newStockToken().getPosition( m_walletAddr) );
	}
	
	public LiveOrderStatus status() {
		return m_status;
	}

	/** Called when the stock order is filled or we receive an update from the Fireblocks server.
	 *  The status is already logged before we come here  
	 * @param hash blockchain hash
	 * @param id Fireblocks id */
	synchronized void onUpdateStatus(String id, FireblocksStatus stat, String hash) {
		
		// needed: date/time, action, qty, symbol, price
		
		
		
		if (stat == FireblocksStatus.CONFIRMING || stat == FireblocksStatus.COMPLETED) {
			onFilled();
		}
		else if (stat.pct() == 100) {
			onFail( new Exception( "The blockchain transaction failed with status " + stat) );
			
			// informational only; don't throw an exception
			try {
				onBlockchainOrderFailed();
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}
		else {
			m_progress = stat.pct();
		}
	}

	/** Called during testing if we bypass the FB processing */
	synchronized void onFilled() {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Filled;
			m_progress = 100;
			m_description = m_description
					.replace("Buy", "Bought")
					.replace("Sell", "Sold");
		}
	}

	/** Called when an error occurs after the order is submitted to IB */
	synchronized void onFail(Exception e) {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Failed;
	
			m_description = e.getMessage();
			if (e instanceof RefException) {
				m_errorCode = ((RefException)e).code();
			}
		}
	}

	/** Called when the user queries status of live orders */
	public synchronized JsonObject getWorkingOrder() {
		String description = S.format("%s %s %s for $%s",
				isBuy() ? "Buy" : "Sell",
				m_desiredQuantity, 
				m_stock.getSymbol(), 
				m_stablecoinAmt); 
		
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "action", isBuy() ? "Buy" : "Sell");
		order.put( "description", description);
		order.put( "progress", m_progress);
		return order;
	}

	/** Called when the user queries status of live orders */
	public synchronized JsonObject getCompletedOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "type", m_status == LiveOrderStatus.Failed ? "error" : "message");   
		order.put( "text", m_description);
		order.put( "status", m_status.toString() );
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}
	
}
