package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.Random;
import java.util.Vector;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController.IOrderHandler;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import common.Util.ExRunnable;
import fireblocks.Accounts;
import fireblocks.StockToken;
import reflection.TradingHours.Session;
import tw.util.S;
import util.LogType;

public class OrderTransaction extends MyTransaction implements IOrderHandler {
	enum LiveOrderStatus { Working, Filled, Failed };

	private static PositionTracker positionTracker = new PositionTracker(); 

	private final Order m_order = new Order();;
	private double m_desiredQuantity;  // same as Order.m_totalQuantity, but this one is set first
	private double m_filledShares;
	private Stock m_stock;
	private double m_stablecoinAmt;
	private double m_tds;
	private boolean m_ibOrderCompleted;
	
	// live order fields
	private String m_errorText = "";  // returned with live orders if order fails
	private LiveOrderStatus m_status = LiveOrderStatus.Working;
	private int m_progress = 5;
	private RefCode m_errorCode; // set if live order fails
	
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
		m_walletAddr = m_map.getRequiredParam("wallet_public_key");
		require( Util.isValidAddress(m_walletAddr), RefCode.INVALID_REQUEST, "The wallet address '%s' is invalid", m_walletAddr);
		jlog( LogType.REC_ORDER, m_map.obj() );
		
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker");

		Action side = m_map.getEnumParam("action", Action.values() );
		require( m_config.allowTrading().allow(side), RefCode.TRADING_HALTED, "Trading is temporarily halted. Please try your order again later.");
		require( m_main.validWallet( m_walletAddr, side), RefCode.ACCESS_DENIED, "Your order cannot be processed at this time (L9)");  // make sure wallet is not blacklisted

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
		m_stock = m_main.getStock(conid);  // throws exception if conid is invalid
		require( m_stock.getAllow().allow(side), RefCode.TRADING_HALTED, "Trading for this stock is temporarily halted. Please try your order again later.");

		m_desiredQuantity = m_map.getRequiredDouble( "quantity");
		require( m_desiredQuantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");

		double price = m_map.getRequiredDouble( "tokenPrice");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double preCommAmt = price * m_desiredQuantity;
		double maxAmt = side == Action.Buy ? m_config.maxBuyAmt() : m_config.maxSellAmt();
		require( Util.isLtEq(preCommAmt, maxAmt), RefCode.ORDER_TOO_LARGE, "The total amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( maxAmt) ); // this is displayed to user
		require( Util.isGtEq(preCommAmt, m_config.minOrderSize()), RefCode.ORDER_TOO_SMALL, "The amount of your order (%s) is below the minimum allowed order size of %s", S.formatPrice( preCommAmt), S.formatPrice( maxAmt) );
		
		m_map.getEnumParam("currency", Stablecoin.values() ); // confirm that it was sent on the order
		
		// get user profile from DB and validate it
		Profile profile = getProfile(); 
		profile.validate();
		profile.checkKyc( Util.isLtEq(preCommAmt, m_config.nonKycMaxOrderSize() ) );  // if order is above max non-KYC size, verify they have passed KYC
		
		// make sure user is signed in with SIWE and session is not expired
		// only trade and redeem messages need this
		validateCookie(m_walletAddr);
		
		// calculate order price
		double prePrice = side == Action.Buy 
			? price - price * m_config.minBuySpread()
			: price + price * m_config.minSellSpread();
		double orderPrice = Util.round( prePrice);  // round to two decimals
				
		// check trading hours
		Session session = m_main.m_tradingHours.insideAnyHours( 
						m_stock.getBool("is24hour"), 
						m_map.get("simtime") );
		require( session != Session.None, RefCode.EXCHANGE_CLOSED, exchangeIsClosed);

		// check the dates (applies to stock splits only)
		m_main.m_tradingHours.checkSplitDates( m_map.get("simtime"), m_stock.getStartDate(), m_stock.getEndDate() );
		
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( session.toString().toUpperCase() );

		m_order.action( side);
		m_order.totalQty( m_desiredQuantity);
		m_order.lmtPrice( orderPrice);
		m_order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
		m_order.allOrNone(true);  // all or none, we don't want partial fills
		m_order.transmit( true);
		m_order.outsideRth( true);
		m_order.orderRef(m_uid);
		m_main.orderController().prepareOrder( m_order); // set order id
		
		// check TDS calculation
		m_tds = m_map.getDoubleParam("tds");
		
		double myTds = m_order.isBuy() 
				? preCommAmt * .01
				: (preCommAmt - m_config.commission() ) * .01;
		//require( Util.isEq( m_tds, myTds, .01), RefCode.INVALID_REQUEST, "TDS of %s does not match calculated amount of %s", m_tds, myTds); 
		
		m_stablecoinAmt = m_map.getDoubleParam("amount");
		if (m_stablecoinAmt == 0) {
			m_stablecoinAmt = m_map.getDoubleParam("price");  // remove this after frontend is upgraded and change above to "getrequireddouble()"
		}
		
		// add this after the tds is fixed for buy order
		double myStablecoinAmt = m_order.isBuy()
			? preCommAmt + m_config.commission() + m_tds
			: preCommAmt - m_config.commission() - m_tds;
		// fix this-> require( Util.isEq(myStablecoinAmt, m_stablecoinAmt, .001), RefCode.INVALID_REQUEST, "The total order amount of %s does not match the calculated amount of %s", m_stablecoinAmt, myStablecoinAmt);
		
		// confirm that the user has enough stablecoin or stock token in their wallet
		// fix this-> requireSufficientStablecoin(order);		
				
		// check that we have prices and that they are within bounds;
		// do this after checking trading hours because that would
		// explain why there are no prices which should never happen otherwise
		Prices prices = m_main.getStock(conid).prices();
		prices.checkOrderPrice( m_order, orderPrice, m_config);
		
		// ***check that the prices are pretty recent; if they are stale, and order is < .5, we will fill the order with a bad price. pas
		// * or check that ANY price is pretty recent, so we know prices are updating
		
		respond( code, RefCode.OK, "id", m_uid);
		
		// now it is up to the live order system to report success or failure
		// we cannot use wrap() anymore, only shrinkWrap()
		// any problem in here calls onFail() and the order gets unwound
		// only two ways out from here: catch in shrinkWrap() or onFireblocksSuccess()
		shrinkWrap( () -> {
			walletLiveOrders().add( this);

			// update the PositionTracker last; if there is a failure after this, we will 
			// unwind the PositionTracker and unwind the IB order if necessary
			// this happens in the shrinkWrap() catch block
			m_order.roundedQty( positionTracker.buyOrSell( contract.conid(), isBuy(), m_desiredQuantity) );

			insertTransaction();  // this must come after all order values are set and before order is placed to ensure that it happens before we get a response from IB

			// nothing to submit to IB; go straight to blockchain
			if (m_order.roundedQty() == 0) {
				// when placing an order that rounds to zero shares, we must check that the prices 
				// are pretty recent; if they are stale, we will fill the order with a bad price
				require( m_stock.hasRecentPrices(isBuy()), RefCode.STALE_DATA, "There is no recent price for this stock. Please try your order again later or increase the order quantity.");  
				
				jlog( LogType.NO_STOCK_ORDER, m_order.getJsonLog(contract) );
				m_order.status(OrderStatus.Filled);
				onIBOrderCompleted(false, false);
			}
			// AUTO-FILL - for testing only
			else if (m_config.autoFill() ) {
				simulateFill(contract);
			}
			// submit order to IB
			else {
				submitOrder( contract);
			}
		});
	}

	private Profile getProfile() throws Exception {
		JsonArray ar = Main.m_config.sqlQuery( conn -> conn.queryToJson("select * from users where wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );
		require( ar.size() == 1, RefCode.INVALID_USER_PROFILE, "No user record found for wallet %s", m_walletAddr);
		return new Profile(ar.get(0));
	}

	private void simulateFill(Contract contract) throws Exception {		
		require( !m_config.isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
		
		Random rnd = new Random(System.currentTimeMillis());

		m_order.orderId( rnd.nextInt(Integer.MAX_VALUE) );
		m_order.permId( rnd.nextInt(Integer.MAX_VALUE) );
		m_order.status(OrderStatus.Filled);
		m_filledShares = m_order.roundedQty();

		// simulate the trade
		Execution exec = new Execution(
				m_order.orderId(),
				0,  // client id
				"" + rnd.nextInt(),
				"time",
				"acct",
				"exch",
				isBuy() ? "buy" : "sell",
				m_order.roundedQty(),
				m_order.lmtPrice(),
				m_order.permId()
				);
		
		m_main.tradeReport( "FAKE" + rnd.nextInt(), contract, exec);  // you could simulate commission report as well 	

		onIBOrderCompleted( false, true); // you might want to sometimes pass false here when testing
	}

	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require() and no wrap(),
	 *  only shrinkWrap()  */
	private void submitOrder( Contract contract) throws Exception {		
		// place order for rounded quantity
		m_main.orderController().placeOrModifyOrder(contract, m_order, this);

		jlog( LogType.SUBMITTED_TO_IB, m_order.getJsonLog(contract) );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( m_config.orderTimeout(), () -> onOrderTimeout() );
	}
	
	@Override public synchronized void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
			int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		
		if (m_ibOrderCompleted) return;

		shrinkWrap( () -> {
			m_order.status(status);
			olog( LogType.IB_ORDER_STATUS, "status", status);

			// save the number of shares filled
			m_filledShares = filled.toDouble();
			//shares.value = filled.toDouble() - 1;  // to test partial fills

			// better is: if canceled w/ no shares filled, let it go to handle() below

			if (status.isComplete() ) {
				out( "  updating permid to %s", permId);
				m_order.permId(permId);
				Util.wrap( () -> m_main.queueSql( conn -> conn.execWithParams("update transactions set perm_id = '%s'", permId) ) );
				onIBOrderCompleted( false, false);
			}
		});
	}

	@Override public synchronized void onRecOrderError(int errorCode, String errorMsg) {
		if (m_ibOrderCompleted) return;

		shrinkWrap( () -> {
			olog( LogType.ORDER_ERR, 
					"errorCode", errorCode,
					"errorMsg", errorMsg,
					"filled", m_filledShares);

			// if some shares were filled, let orderStatus or timeout handle it
			if (m_filledShares > 0) {
				return;
			}

			// no shares filled

			// price does not conform to market rule, e.g. too many decimals
			if (errorCode == 110) {
				m_ibOrderCompleted = true;
				throw new RefException( RefCode.INVALID_PRICE, errorMsg);
			}

			// order rejected, never sent to exchange; could be not enough liquidity
			if (errorCode == 201) {
				m_ibOrderCompleted = true;
				throw new RefException( RefCode.REJECTED, errorMsg);
			}
		});
	}
	
	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require()
	 *  this is wrapped() but it must call onIBOrderCompleted() so cannot throw an exception  */
	private synchronized void onOrderTimeout() {
		if (m_ibOrderCompleted) return;
		
		shrinkWrap( () -> {
			// this will happen if our timeout is lower than the timeout of the IOC order
			olog( LogType.ORDER_TIMEOUT, 
					"sharesFilled", m_filledShares,
					"orderStatus", m_order.status() );

			// if order is still live, cancel the order; don't let an error here disrupt processing
			if (!m_order.status().isComplete() && !m_order.status().isCanceled() ) {
				Util.wrap( () -> {
					jlog( LogType.CANCEL_ORDER, null);
					m_main.orderController().cancelOrder( m_order.orderId(), "", null);
				});
			}
			onIBOrderCompleted(true, false);
		});
	}

	/** We cannot throw an exception. We have already called respond(), we must now be sure to update the live order */
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  This call is shrinkWrapped()
	 *  In the case where order qty < .5 and we didn't submit an order, orderStatus will be Filled.
	 *  You must (a) make a log entry and (b) either call m_liveOrder.filled() or throw an exception */ 
	private synchronized void onIBOrderCompleted(boolean timeout, boolean simulated) throws Exception {
		if (m_ibOrderCompleted) return;

		m_ibOrderCompleted = true;
		
		// update status here with timeout; if some shares were filled, the blockchain will
		// proceed and the status will be updated further
		

		require(
				m_filledShares > 0 || m_order.status() == OrderStatus.Filled,
				timeout ? RefCode.TIMED_OUT : RefCode.UNKNOWN,
				timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.");
		
		// the order has been filled or partially filled; note that filledShares can be zero if the size was rounded down
		m_progress = FireblocksStatus.STOCK_ORDER_FILLED.pct(); // set m_progress to 15%
		olog( LogType.ORDER_FILLED, "filledShares", m_filledShares, "simulated", simulated);
		
		if (fireblocks() ) {
			// execute this in a new thread because, the theory is, it can take a while
			Util.execute( "FBL", () -> shrinkWrap( () -> startFireblocks(m_filledShares) ) );
		}
		else {
			onFireblocksSuccess();
		}
	}
	
	/** Call to this method is shrinkWrapped();
	 *  any failure in here and the order must be unwound from the PositionTracker;
	 *  Runs in a separate thread so as not to hold up the API thread */ 
	private void startFireblocks(double filledShares) throws Exception {
		// partial fills are not supported
		require( filledShares == m_order.roundedQty(), RefCode.PARTIAL_FILL, "Order failed due to partial fill");

		// for testing
		if (m_map.getBool("fail") ) {
			throw new Exception("Blockchain transaction failed intentially during testing"); 
		}

		out( "Starting Fireblocks protocol");

		String fbId;

		// buy
		if (m_order.isBuy() ) {
			
			// buy with RUSD?
			if (m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.RUSD) {
				fbId = m_config.rusd().buyStockWithRusd(
						m_walletAddr, 
						m_stablecoinAmt,
						newStockToken(),
						m_order.totalQty()
				).id();
			}
			
			// buy with XUSD
			else {
				fbId = m_config.rusd().buyStock(
						m_walletAddr,
						m_config.busd(),
						m_stablecoinAmt,
						newStockToken(), 
						m_order.totalQty()
				).id();
			}
		}
		
		// sell
		else {
			fbId = m_config.rusd().sellStockForRusd(
					m_walletAddr,
					m_stablecoinAmt,
					newStockToken(),
					m_order.totalQty()
			).id();
		}
		
		// the FB transaction has been submitted; there is a little window here where an
		// update from FB could come and we would miss it because we have not added the
		// id to the map yet; we could fix this with synchronization
		allLiveOrders.put(fbId, this);

		// update transaction table with fireblocks id
		m_main.queueSql( conn -> conn.execute( 
				String.format("update transactions set fireblocks_id = '%s' where uid = '%s'",
					fbId, m_uid) ) );
		
		olog( LogType.SUBMITTED_TO_FIREBLOCKS, 
				"id", fbId, 
				"currency", m_map.getParam("currency"),
				"adminId", Accounts.instance.getAdminAccountId(m_walletAddr) );
		
		// if we don't make it to here, it means there was an exception which will be picked up
		// by shrinkWrap() and the live order will be failed()
	}
	
	/** Called when we receive an update from the Fireblocks server.
	 *  The status is already logged before we come here  
	 * @param hash blockchain hash
	 * @param id Fireblocks id */
	synchronized void onUpdateFbStatus(FireblocksStatus stat) {
		if (stat == FireblocksStatus.CONFIRMING || stat == FireblocksStatus.COMPLETED) {
			onFireblocksSuccess();
		}
		else if (stat.pct() == 100) {
			try {
				// the blockchain transaction has failed; try to determine why

				// confirm that the user has enough stablecoin or stock token in their wallet
				requireSufficientCrypto();
				
				// confirm that user approved purchase with USDC 
				requireSufficientApproval();
				
				// unknown blockchain error
				onFail( "The blockchain transaction failed with status " + stat, null);
			}
			catch( RefException e) {
				onFail( e.getMessage(), e.code() );
			}
			catch( Exception e) {
				e.printStackTrace();  // not good, it means there was an exception when trying to determin why the blockchain transaction failed
				onFail( e.getMessage(), null);
			}

			// write to log file (don't throw, informational only)
			// this is second log entry for the failed order (see onFail() )
			try {
				// this is not good, I think it sends the wallet query several time unnecessarily. pas
				
				// this is not ideal because it will query the balances again which we just queried
				// above when trying to determine why the order failed; should be rare, though
				olog( LogType.BLOCKCHAIN_FAILED, 
						"desired", m_desiredQuantity,
						"approved", Main.m_config.busd().getAllowance( m_walletAddr, Main.m_config.rusdAddr() ),
						"USDC", Main.m_config.busd().getPosition(m_walletAddr),
						"RUSD", Main.m_config.rusd().getPosition(m_walletAddr),
						"stockToken", newStockToken().getPosition( m_walletAddr) );
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}
		else {
			m_progress = stat.pct();
		}
	}
	
//	1. all of the trans.status updates must go into the database thread
//	2. why does transaction.status say FAILED but there is no corr. log entry;
//	   note there is no FbActionServer to report any fireblocks status

	/** Called when blockchain goes to CONFIRMING or COMPLETED;
	 *  also called during testing if we bypass the FB processing;
	 *  set up the order so that user will received Filled msg on next update */
	synchronized void onFireblocksSuccess() {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Filled;
			m_progress = 100;
			jlog( LogType.ORDER_COMPLETED, null);
		}
	}

	/** Called when an error occurs after the order is submitted to IB, whether it filled or not;
	 *  could come from IB, Fireblocks, or any other exception */ 
	synchronized void onFail(String errorText, RefCode errorCode) {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Failed;
			
			// create log entry and update transactions table
			olog( LogType.ORDER_FAILED, Message, errorText, "code", errorCode);
			
			m_main.queueSql( sql -> sql.execWithParams( 
					"update transactions set status = '%s' where uid = '%s'", FireblocksStatus.FAILED, m_uid) );
		
			// unwind PositionTracker and IB order first and foremost
			unwindOrder();
	
			// save error text and code which will be sent back to client when they query live order status
			m_errorText = errorText;
			m_errorCode = errorCode;

			// send alert, but not when testing, and don't throw an exception, it's just reporting
			if (!m_map.getBool("testcase")) {
				alert( "ORDER FAILED", String.format( "uid=%s  text=%s  code=%s", m_uid, m_errorText, m_errorCode) );
			}
		}
	}	

	/** Insert into transaction table; called when the IB order is submitted */
	private void insertTransaction() {
		try {
			JsonObject obj = new JsonObject();
			obj.put("uid", m_uid);
			obj.put("order_id", m_order.orderId() );  // ties the order to the trades
			obj.put("perm_id", m_order.permId() );    // have to make sure this is set. pas
			obj.put("wallet_public_key", m_walletAddr);
			obj.put("action", m_order.action() ); // enums gets quotes upon insert
			obj.put("quantity", m_order.totalQty());
			obj.put("rounded_quantity", m_order.roundedQty() );
			obj.put("symbol", m_stock.getSymbol() );
			obj.put("conid", m_stock.getConid() );
			obj.put("price", m_order.lmtPrice() );
			obj.put("commission", m_config.commission() ); // not so good, we should get it from the order. pas
			obj.put("tds", m_tds);
			obj.put("status", FireblocksStatus.LIVE); // this is now a live order and we are waiting for IB and/or Blockchain
			obj.put("currency", m_map.getEnumParam("currency", Stablecoin.values() ).toString() );
		
			m_main.queueSql( conn -> conn.insertJson("transactions", obj) );
		} 
		catch (Exception e) {
			elog( LogType.DATABASE_ERROR, e);
			e.printStackTrace();
		}
	}

	/** Confirm that the user has enough stablecoin or stock token in their wallet.
	 *  This could be called before or after submitting the stock order */
	private void requireSufficientCrypto() throws Exception {
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

	private void requireSufficientApproval() throws Exception {
		// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
		if (m_order.isBuy() && m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC) {
			double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() ); 
			require( 
					Util.isGtEq(approvedAmt, m_stablecoinAmt), 
					RefCode.INSUFFICIENT_ALLOWANCE,
					"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, m_stablecoinAmt); 
		}
		
	}

	private StockToken newStockToken() throws Exception {
		return new StockToken( m_stock.getSmartContractId() );
	}

	/** The order was submitted. It may have been filled, maybe not. We must unwind the order from the
	 *  PositionTracker and submit a reverse order if necessary. */ 
	private void unwindOrder() {
		try {
			int conid = m_map.getRequiredInt("conid");

			// if no shares were filled, just remove the balances from the position tracker
			if (m_filledShares == 0) {
				out( "Unwinding order from PositionTracker"); 
				positionTracker.undo( conid, isBuy(), m_order.totalQty(), m_order.roundedQty() );
			}
			
			// if shares were filled, have to execute an opposing trade
			else {
				Contract contract = new Contract();
				contract.conid( conid);
				contract.exchange( m_main.getExchange( contract.conid() ) );
			
				m_main.orderController().prepareOrder(m_order);  // reset order id
				m_order.orderType(OrderType.MKT); // this won't work off-hours
				m_order.flipSide();
				m_order.roundedQty(  // use the PositionTracker to determine number of shares to buy or sell; it may be different from the original number if other orders have filled in between 
						positionTracker.buyOrSell( contract.conid(), m_order.isBuy(), m_order.totalQty() ) ); 
			
				if (m_order.roundedQty() > 0 && !m_config.autoFill() ) {
					m_main.orderController().placeOrModifyOrder(contract, m_order, null);
				}
			}
		}
		catch( Exception e) {
			elog( LogType.UNWIND_ERROR, e);
			e.printStackTrace();
			alert( "Error occurred while unwinding order", e.getMessage() );
		}
	}

	private boolean fireblocks() throws RefException {
		return m_config.isProduction() || m_config.useFireblocks() && !m_map.getBool("noFireblocks");
	}

	private Vector<OrderTransaction> walletLiveOrders() {
		return Util.getOrCreate(liveOrders, m_walletAddr.toLowerCase(), () -> new Vector<OrderTransaction>() );
	}
	
	/** Like wrap, but instead of notifying the http client, we unwind the IB order */
	private void shrinkWrap(ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			out( e);
			elog( LogType.EXCEPTION, e);
			onFail(e.getMessage(), e.code() );
		}
		catch( Exception e) {
			e.printStackTrace();
			elog( LogType.EXCEPTION, e);
			onFail(e.getMessage(), null);
		}
	}
	
	boolean isBuy() {
		return m_order.isBuy();  // null exception here? don't call isBuy() until m_order is set in order() method, or change isBuy() to call action()
	}

	public LiveOrderStatus status() {
		return m_status;
	}

	/** Called when the monitor program queries for all live orders */
	public synchronized JsonObject getLiveOrder() {
		JsonObject order = new JsonObject();
		order.put( "uid", uid() );
		order.put( "wallet", m_walletAddr);
		order.put( "action", m_order.action() );
		order.put( "description", getWorkingOrderText() );
		order.put( "progress", m_progress);
		order.put( "status", m_status.toString() );
		order.put( "errorText", m_errorText);
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}

	/** Called when Frontend queries live order for a single wallet */
	public synchronized JsonObject getWorkingOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "action", m_order.action().toString().toLowerCase() ); // front end requires lower case to set the right color
		order.put( "description", getWorkingOrderText() );
		order.put( "progress", m_progress);
		return order;
	}

	/** Called when Frontend queries live order for a single wallet */
	public synchronized JsonObject getCompletedOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "type", m_status == LiveOrderStatus.Failed ? "error" : "message");   
		order.put( "status", m_status.toString() );
		order.put( "text", m_status == LiveOrderStatus.Failed ? m_errorText : getCompletedOrderText() );
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}
	
	private String getWorkingOrderText() {
		return S.format( "%s %s %s for %s",
				m_order.action(), m_desiredQuantity, m_stock.getSymbol(), m_stablecoinAmt);
	}
	
	private String getCompletedOrderText() {
		return S.format( "%s %s %s for %s",
				isBuy() ? "Bought" : "Sold", m_desiredQuantity, m_stock.getSymbol(), m_stablecoinAmt);
	}

	@Override public void orderState(OrderState orderState) {
		// ignore this, we don't care
	}
	
	/** If we come here, it mean the order failed before it went to the LIVE state,
	 *  so no entry was made in the transactions table. Insert a placeholder into
	 *  the transactions table so we have a record of the order. */
	@Override protected void postWrap() {
		m_main.queueSql( conn -> conn.insertJson("transactions", 
				Util.toJson( 
						"uid", m_uid,
						"wallet_public_key", m_walletAddr,
						"status", FireblocksStatus.DENIED) ) );
	}
}
// look at all the catch blocks, save message or stack trace
// you have to not log the cookie
// check logs for FB updates
// for log entry, you can trim the wallet