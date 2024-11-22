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

import chain.Stocks.Stock;
import common.SmtpSender;
import common.Util;
import common.Util.ExRunnable;
import reflection.TradingHours.Session;
import reflection.UserTokenMgr.UserToken;
import telegram.Telegram;
import tw.util.S;
import util.LogType;
import web3.Erc20;
import web3.RetVal;
import web3.Stablecoin;
import web3.StockToken;

public class OrderTransaction extends MyTransaction implements IOrderHandler, LiveTransaction {
	enum LiveOrderStatus { Working, Filled, Failed };

	private static PositionTracker positionTracker = new PositionTracker(); 

	private final Order m_order = new Order();
	private double m_desiredQuantity;  // decimal desired quantity
	private double m_filledShares;
	private Stock m_stock;		// for prices
	private StockToken m_stockToken;			// for placing the order (has correct contract id)
	private double m_stablecoinAmt;
	private double m_tds;
	private boolean m_ibOrderCompleted;
	private Stablecoin m_stablecoin;  // stablecoin (upper-case) being used to purchase the stock token
	private String m_email;
	
	// live order fields
	private String m_errorText = "";  // returned with live orders if order fails
	private LiveOrderStatus m_status = LiveOrderStatus.Working;   // move up to base class
	private int m_progress = 5;  // only relevant if status is working
	private RefCode m_errorCode; // set if live order fails
	private String m_message; // a message that will be displayed to user for a live (not completed) order
	private long m_createdAt = System.currentTimeMillis();
	private double m_sourceTokenQty; // if set, it means that we added our source token quantity to the UserTokenMgr, and we must subtract it out when the order is completed

	public OrderTransaction(Main main, HttpExchange exch) {
		super(main, exch);
 	}
	
	@Override public String tableName() {
		return "transactions";
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
		m_walletAddr = m_map.getWalletAddress("wallet_public_key");
		
		jlog( LogType.REC_ORDER, m_map.obj() );
		
		// make sure user is signed in with SIWE and session is not expired
		// must come before profile and KYC checks
		validateCookie("order");  // set m_chain for retrieval with chain()
		setChainFromHttp();
		
		require( !isBlockedIP(), RefCode.BLOCKED_IP, "Trading is not supported from your location"); // OR user is connecting through a VPN or Data Center
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected; please try your order again later");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker; please try your order again later");

		Action side = m_map.getEnumParam("action", Action.values() );
		require( m_config.allowTrading().allow(side), RefCode.TRADING_HALTED, "Trading is temporarily halted; please try your order again later");
		require( m_main.validWallet( m_walletAddr, side), RefCode.ACCESS_DENIED, "Your order cannot be processed at this time (L9)");  // make sure wallet is not blacklisted

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
		m_stock = m_main.getStock( conid);  // throws exception if conid is invalid
		m_stockToken = chain().getTokenByConid( conid);  // throws exception if conid is invalid
		require( m_stockToken.rec().allow().allow(side), RefCode.TRADING_HALTED, "Trading for this stock is temporarily halted. Please try your order again later.");

		m_desiredQuantity = m_map.getRequiredDouble( "quantity");
		require( m_desiredQuantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");

		double price = m_map.getRequiredDouble( "tokenPrice");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double preCommAmt = price * m_desiredQuantity;
		require( Util.isGtEq(preCommAmt, m_config.minOrderSize()), RefCode.ORDER_TOO_SMALL, "The amount of your order (%s) is below the minimum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( m_config.minOrderSize()) ); // displayed to user
		require( Util.isLtEq(preCommAmt, m_config.maxOrderSize()), RefCode.ORDER_TOO_LARGE, "The amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( preCommAmt), S.formatPrice( m_config.maxOrderSize()) ); // displayed to user
				
		// set m_stablecoin from currency parameter; must be RUSD or non-RUSD
		String currency = m_map.getRequiredString("currency").toUpperCase();
		if (currency.equals( chain().rusd().name() ) ) {
			m_stablecoin = chain().rusd();
		}
		else if (currency.equals( chain().busd().name() ) ) {
			m_stablecoin = chain().busd();
		}
		require( m_stablecoin != null, RefCode.INVALID_REQUEST, "Invalid currency");
			
		// NOTE: this next code is the same as OrderTransaction


		// get record from Users table
		JsonArray ar = Main.m_config.sqlQuery( conn -> conn.queryToJson("select * from users where wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );  // note that this returns a map with all the null values
		require( ar.size() == 1, RefCode.INVALID_USER_PROFILE, "Please update your profile and then resubmit your order");

		// validate user profile fields
		JsonObject userRecord = ar.get(0);
		Profile profile = new Profile(userRecord);
		profile.validate();
		
		// save email to send alerts later
		m_email = profile.email(); 

		// validate KYC fields
		require( 
				Util.isLtEq(preCommAmt, m_config.nonKycMaxOrderSize() )
//				&& getCountryCode().equals( "IN")
				
				|| Util.equalsIgnore( userRecord.getString("kyc_status"), "VERIFIED", "completed"),
				
				RefCode.NEED_KYC,
				"Please verify your identity and then resubmit your order");
		
		// calculate IB order price
		double prePrice = side == Action.Buy 
			? price - price * m_config.minBuySpread()
			: price + price * m_config.minSellSpread();
		double orderPrice = Util.round( prePrice);  // round to two decimals
				
		// check trading hours
		Session session = m_main.m_tradingHours.getTradingSession( 
						m_stock.rec().is24Hour(), 
						m_map.get("simtime") );
		require( session != Session.None, RefCode.EXCHANGE_CLOSED, exchangeIsClosed);

		// check the dates (applies to stock splits only)
		m_main.m_tradingHours.checkSplitDates( m_map.get("simtime"), m_stock.rec().startDate(), m_stock.rec().endDate() );
		
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( session.toString().toUpperCase() );
		
		// special case: for crypto, set exchange to PAXOS
		if (m_stock.rec().type().equals( "Crypto") ) {
			contract.exchange( "PAXOS");
		}

		m_order.action( side);
		m_order.lmtPrice( orderPrice);
		m_order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
		m_order.transmit( true);
		m_order.outsideRth( true);
		m_order.orderRef(m_uid);
		// note that allOrNone is not supported for overnight session 
		
		// check TDS calculation
		m_tds = m_map.getDoubleParam("tds");
		
		m_stablecoinAmt = m_map.getDoubleParam("amount");  // incorporates commission and tds
		if (m_stablecoinAmt == 0) {
			m_stablecoinAmt = m_map.getDoubleParam("price");  // remove this after frontend is upgraded and change above to "getrequireddouble()"
		}
		
		// check the stablecoin amt calculation
		double myStablecoinAmt = m_order.isBuy()
			? preCommAmt + m_config.commission() + m_tds
			: preCommAmt - m_config.commission() - m_tds;
		require( 
				Util.isEq(myStablecoinAmt, m_stablecoinAmt, .05),  // +/- two cents; for some reason, Front end is sometimes off by .01; there's a bug assigned, but in the meantime, don't sweat it 
				RefCode.INVALID_REQUEST, 
				"The total order amount of %s does not match the calculated amount of %s", m_stablecoinAmt, myStablecoinAmt);
		
		// check that we have prices and that they are within bounds;
		// do this after checking trading hours because that would
		// explain why there are no prices which should never happen otherwise
		Prices prices = m_stock.prices();
		prices.checkOrderPrice( m_order, orderPrice);
		
		// check that user has sufficient crypto to buy or sell
		// must come after m_stablecoin is set
		// NOTE that this may revise m_stablecoinAmt
		requireSufficientCrypto( true);
		
		respond( code, RefCode.OK, "id", m_uid); // Frontend will display a message which is hard-coded in Frontend
		
		// now it is up to the live order system to report success or failure
		// we cannot use wrap() anymore, only shrinkWrap()
		// any problem in here calls onFail() and the order gets unwound
		// only two ways out from here: catch in shrinkWrap() or onFireblocksSuccess()
		shrinkWrap( () -> {
			// sync access because items are added and removed by different threads
			synchronized( walletLiveOrders() ) {
				walletLiveOrders().add( this);
			}

			// update the PositionTracker last; if there is a failure after this, we will 
			// unwind the PositionTracker and unwind the IB order if necessary
			// this happens in the shrinkWrap() catch block
			m_order.roundedQty( positionTracker.buyOrSell( contract.conid(), isBuy(), m_desiredQuantity) );

			insertTransaction();  // this must come after all order values are set and before order is placed to ensure that it happens before we get a response from IB

			// nothing to submit to IB?
			if (m_order.roundedQty() == 0) {
				// when placing an order that rounds to zero shares, we must check that the prices 
				// are pretty recent; if they are stale, we could fill the order with a bad price
				require(
						System.currentTimeMillis() - m_stock.prices().time() <= m_config.recentPrice(),
						RefCode.STALE_DATA, 
						"There is no recent price for this stock. Please try your order again later or increase the order quantity.");  
				
				// go straight to blockchain
				jlog( LogType.NO_STOCK_ORDER, m_order.getJsonLog(contract) );
				m_order.status(OrderStatus.Filled);
				onIBOrderCompleted(false, false);
			}
			// AUTO-FILL - for testing only
			else if (chain().params().autoFill() ) {
				simulateFill(contract);
			}
			// submit order to IB
			else {
				submitOrder( contract);
			}
		});
	}

	private void simulateFill(Contract contract) throws Exception {		
		require( !chain().params().isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
		
		Random rnd = new Random(System.currentTimeMillis());
		
		int simPartial = m_map.getInt("simPartial");

		m_order.orderId( rnd.nextInt(Integer.MAX_VALUE) );
		m_order.permId( rnd.nextInt(Integer.MAX_VALUE) );
		m_order.status(OrderStatus.Filled);
		m_filledShares = simPartial > 0 ? simPartial : m_order.roundedQty();  // simPartial can be passed by the test cases

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
		m_main.orderController().placeOrder(contract, m_order, this);

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

			// better is: if canceled w/ no shares filled, let it go to handle() below

			if (status.isComplete() ) {
				out( "  updating permid to %s", permId);
				
				m_order.permId(permId);
				
				Util.wrap( () -> m_main.queueSql( conn -> 
					conn.execWithParams("update transactions set perm_id = '%s' where uid = '%s'", permId, m_uid) ) );
				
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
			if (errorCode == 201 || errorCode == 103) {  // 103 = dup order id
				m_ibOrderCompleted = true;
				throw new RefException( RefCode.REJECTED, errorMsg);
			}
			
			// add other fatal errors here as we see them; maybe some errors are non-fatal
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

		// partial fills
		if (m_filledShares < m_order.roundedQty() ) {
			
			m_message = "Your order was partially filled and is now being processed on the blockchain";  // display toast to user

			double ratio = m_filledShares / Math.min(m_order.roundedQty(), m_desiredQuantity);  // by using the smaller of the two, we will give the user a slightly higher percentage fill for a better user experience
			olog( LogType.PARTIAL_FILL, "desiredQty", m_desiredQuantity, "filledQty", m_filledShares, "ratio", ratio);

			// reject and unwind if we filled less than half; this is debatable 
			// require( ratio >= m_config.minPartialFillPct(), RefCode.PARTIAL_FILL, "Order failed due to partial fill");
			
			m_stablecoinAmt *= ratio;
			m_desiredQuantity *= ratio;
			m_tds *= ratio;
			
			// we could update the UserTokenMgr here if desired; it would be more accurate
			
			updateAfterPartialFill(ratio);
		}
		else {
			m_message = "Your order was filled and is now being processed on the blockchain";  // display toast to user
			olog( LogType.ORDER_FILLED, "filledShares", m_filledShares, "simulated", simulated);
		}
		
		// set m_progress to 15%
		m_progress = FireblocksStatus.STOCK_ORDER_FILLED.pct();

		// execute this in a new thread because it can take a while and we don't want to tie
		// up the IB execution thread
		Util.execute( "FBL", () -> shrinkWrap( () -> startFireblocks(m_filledShares) ) );
	}
	
	/** Call to this method is shrinkWrapped();
	 *  any failure in here and the order must be unwound from the PositionTracker;
	 *  Runs in a separate thread so as not to hold up the API thread */ 
	private void startFireblocks(double filledShares) throws Exception {
		// for testing
		if (m_map.getBool("fail") ) {
			S.sleep(15000);
			throw new Exception("Blockchain transaction failed intentially during testing"); 
		}

		out( "Starting Fireblocks protocol");

		RetVal retval;

		// buy
		if (m_order.isBuy() ) {
			retval = chain().rusd().buyStock(
					m_walletAddr,
					m_stablecoin,
					m_stablecoinAmt,
					m_stockToken, 
					m_desiredQuantity
			);
		}
		
		// sell
		else {
			retval = chain().rusd().sellStockForRusd(
					m_walletAddr,
					m_stablecoinAmt,
					m_stockToken,
					m_desiredQuantity
			);
		}
		
		out( "Submitted blockchain order with id/hash=%s", retval.hash() );
		
		// the FB transaction has been submitted; there is a little window here where an
		// update from FB could come and we would miss it because we have not added the
		// id to the map yet; we could fix this with synchronization
		allLiveTransactions.put(retval.hash(), this);

		// REFBLOCKS: wait for the transaction receipt (with polling)
		// note this does not query for the real error message text if it fails;
		// we could add that later if desired
		try {
			out( "waiting for blockchain hash");
			String hash = retval.waitForReceipt();
			out( "blockchain transaction completed with hash %s", hash);
			
			// update hash in table; for Fireblocks, this is done when we receive a message 
			// from the fbserver before onUpdateFbStatus() is called
			m_main.queueSql( sql -> sql.execWithParams( 
					"update transactions set blockchain_hash = '%s' where uid = '%s'", 
					hash, m_uid) );
			
			onUpdateFbStatus( FireblocksStatus.COMPLETED, hash);
		}
		catch( Exception e) {
			out( "blockchain transaction failed " + e.getMessage() );
			e.printStackTrace();
			onUpdateFbStatus( FireblocksStatus.FAILED_WEB3, null); // tries to guess why it failed and then calls onFail()
		}
		
		// if we don't make it to here, it means there was an exception which will be picked up
		// by shrinkWrap() and the live order will be failed()
	}

	private void updateAfterPartialFill(double ratio) {
		try {
			out( "Updating transaction after partial fill with ratio %s", ratio);
			
			JsonObject obj = new JsonObject();
			obj.put("quantity", m_desiredQuantity);
			obj.put("commission", m_config.commission() * ratio); // not so good, we should get it from the order. pas
			obj.put("tds", m_tds);
			m_main.queueSql( conn -> conn.updateJson("transactions", obj, "uid = '%s'", m_uid) );
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Called when we receive an update from the Fireblocks server OR now
	 *  when we receive the receipt for the Refblocks transaction.
	 *  The status is already logged before we come here  
	 * @param hash blockchain hash
	 * @param id Fireblocks id */
	@Override public synchronized void onUpdateFbStatus(FireblocksStatus stat, String hash) {
		// remove this order from UserTokenMgr once it completes or hits the CONFIRMING state
		// it's not perfect as the change in balance of the source token due to this order
		// might not have updated yet, but it should be shortly hereafter
		if (stat == FireblocksStatus.CONFIRMING || stat.pct() == 100) {
			removeFromUserTokenMgr();
		}
		
		if (stat == FireblocksStatus.COMPLETED) {
			onFireblocksSuccess(hash);
		}
		else if (stat.pct() == 100) {
			// write to log file (don't throw, informational only)
			wrap( () -> {
				// this is not good, I think it sends the wallet query several time unnecessarily. pas
				// this is not ideal because it will query the balances again which we just queried
				// above when trying to determine why the order failed; should be rare, though
				olog( LogType.BLOCKCHAIN_FAILED, 
						"desired", m_desiredQuantity,
						"approved", chain().busd().getAllowance( m_walletAddr, chain().rusd().address() ),
						"USDC", chain().busd().getPosition(m_walletAddr),
						"RUSD", chain().rusd().getPosition(m_walletAddr),
						"stockToken", m_stockToken.getPosition( m_walletAddr) );
			});

			try {
				// the blockchain transaction has failed; try to determine why

				// confirm that the user has enough stablecoin or stock token in their wallet
				requireSufficientCrypto(false);
				
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
		}
		else {
			m_progress = stat.pct();
		}
	}
	
//	1. all of the trans.status updates must go into the database thread
//	2. why does transaction.status say FAILED but there is no corr. log entry;
//	   note there is no FbActionServer to report any fireblocks status

	/** Called when blockchain goes to COMPLETED;
	 *  also called during testing if we bypass the FB processing;
	 *  set up the order so that user will received Filled msg on next update
	 *  @param hash is the blockchain hash, could be null in development */
	synchronized void onFireblocksSuccess(String hash) {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Filled;
			m_progress = 100;

			// really take these out? where did they move to???
			jlog( LogType.ORDER_COMPLETED, Util.toJson( "hash", hash) );

			m_main.queueSql( sql -> sql.execWithParams( 
					"update transactions set status = '%s' where uid = '%s'", FireblocksStatus.COMPLETED, m_uid) );

			// send alert, but not when testing, and don't throw an exception, it's just reporting
			Util.wrap( () -> {
				if (chain().params().isProduction() && !m_map.getBool("testcase")) {
					alert( "ORDER COMPLETED", getCompletedOrderText() + " " + m_walletAddr );
					
					Util.wrap( () -> {
						Util.require( Util.isValidEmail(m_email), "Error: invalid email " + m_email); // should never happen 
						
						// send email to the user
						String html = S.format( isBuy() ? buyConf : sellConf,
								m_desiredQuantity,
								m_stock.symbol(),
								m_stablecoinAmt,
								m_stablecoin.name(),
								m_stockToken.address(),
								chain().blockchainTx( hash) );
						m_config.sendEmailSes(m_email, "Order filled on Reflection", html, SmtpSender.Type.Trade);
					});
				}
			});
			
			if (m_config.sendTelegram() ) {
				Util.wrap( () -> {
					out( "sending telegram");
					String str = String.format( "Wallet %s just %s [%s %s stock tokens](%s) on %s!",
							Util.shorten( m_walletAddr), 
							isBuy() ? "bought" : "sold", 
							m_desiredQuantity, 
							m_stock.symbol(), 
							chain().blockchainTx( hash), 
							chain().params().name()	);
					
					Telegram.postPhoto( str, isBuy() ? Telegram.bought : Telegram.sold);
				});
			}
			else {
				out( "not sending telegram");
			}
		}
	}

	/** Called when an error occurs after the order is submitted to IB, whether it filled or not;
	 *  could come from IB, Fireblocks, or any other exception */ 
	synchronized void onFail(String errorText, RefCode errorCode) {
		// subtract out this order from the UserTokenMgr if necessary
		removeFromUserTokenMgr();
		
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Failed;
			
			// create log entry and update transactions table
			olog( LogType.ORDER_FAILED, Message, errorText, "code", errorCode);
			
			JsonObject trans = Util.toJson(
					"status", FireblocksStatus.FAILED,
					"order_id", m_order.orderId(),
					"perm_id", m_order.permId(),
					"ref_code", errorCode);
					
			m_main.queueSql( sql -> sql.updateJson(
					"transactions",
					trans,
					"uid = '%s'",
					m_uid) );
			
			// unwind PositionTracker and IB order first and foremost
			unwindOrder();
	
			// save error text and code which will be sent back to client when they query live order status
			m_errorText = errorText;
			m_errorCode = errorCode;

			// send alert, but not when testing, and don't throw an exception, it's just reporting
			try {
				if (!chain().params().isProduction() && !m_map.getBool("testcase")) {
					alert( "ORDER FAILED", String.format( "uid=%s  text=%s  code=%s  wallet=%s", m_uid, m_errorText, m_errorCode, m_walletAddr) );
				}
			}
			catch( Exception e) {
				S.out( "Could not send alert - " + e.getMessage() );
			}
		}
	}	

	/** Insert into transaction table; called when the IB order is submitted;
	 *  note that order id and perm id are not available yet */
	private void insertTransaction() {
		try {
			JsonObject obj = new JsonObject();
			obj.put("uid", m_uid);
			obj.put("wallet_public_key", m_walletAddr);
			obj.put("chainId", chain().chainId() );
			obj.put("action", m_order.action() ); // enums gets quotes upon insert
			obj.put("quantity", m_desiredQuantity);
			obj.put("rounded_quantity", m_order.roundedQty() );
			obj.put("symbol", m_stock.symbol() );
			obj.put("conid", m_stock.conid() );
			obj.put("price", m_order.lmtPrice() );
			obj.put("commission", m_config.commission() ); // not so good, we should get it from the order. pas
			obj.put("tds", m_tds);
			obj.put("status", FireblocksStatus.LIVE); // this is now a live order and we are waiting for IB and/or Blockchain
			obj.put("currency", m_stablecoin.name() );
			obj.put("country", getCountryCode() );
			obj.put("ip_address", getUserIpAddress() );
		
			m_main.queueSql( conn -> conn.insertJson("transactions", obj) );
			
			PortfolioTransaction.processTrans( m_walletAddr, obj);
		} 
		catch (Exception e) {
			elog( LogType.DATABASE_ERROR, e);
			e.printStackTrace();
		}
	}

	/** Confirm that the user has enough stablecoin or stock token in their wallet.
	 *  This could be called before or after submitting the stock order */
	private void requireSufficientCrypto(boolean before) throws Exception {

		// get the position; if not available, let the order proceed; if the blockchain order fails, so be it
		JsonObject positionsMap;  // keys are native, approved, positions, wallet (addr)

		// get current balance of source token from RPC node (we could also get it from HookServer)
		var sourceToken = getSourceToken();
		double balance = sourceToken.getPosition( m_walletAddr);  // sends query

		// adjust the balance to consider live orders
		if (before) {
			UserToken userToken = UserTokenMgr.getUserToken( m_walletAddr, sourceToken.address() );
	
			double needed = m_order.isBuy() ? m_stablecoinAmt : m_desiredQuantity;  // neeeded qty of source token 
			out( "Checking balance prior to order:  balance=%s  offset=%s  net=%s  needed=%s", 
					balance, userToken.offset(), balance - userToken.offset(), needed);
	
			balance -= userToken.increment( needed);
			m_sourceTokenQty = needed;
			
			// check for sufficient crypto balance but cut them some slack to account for rounding errors 
			// at frontend; if they are short by a little, reduce the stablecoin amount to match the balance;
			// saw this in production for RUSD but not for stock tokens
			if (m_order.isBuy() ) {
				if (m_stablecoinAmt > balance && m_stablecoinAmt - balance < .02) {
					out( "Adjusting stablecoin amount from %s to %s", 
							S.fmt6( m_stablecoinAmt), S.fmt6( balance) );
					m_stablecoinAmt = balance;
				}
			}
		}

		if (m_order.isBuy() ) {
			require( Util.isGtEq(balance, m_stablecoinAmt), 
					RefCode.INSUFFICIENT_STABLECOIN,
					"The stablecoin balance (%s) is less than the total order amount (%s)", 
					balance, m_stablecoinAmt );
		}
		else {
			require( Util.isGtEq(balance, m_desiredQuantity), 
					RefCode.INSUFFICIENT_STOCK_TOKEN,
					"The stock token balance (%s) is less than the order quantity (%s)", 
					balance, m_desiredQuantity);
		}
	}
	
	private Erc20 getSourceToken() {
		return m_order.isBuy() ? m_stablecoin : m_stockToken;
	}

	private void requireSufficientApproval() throws Exception {
		// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
		// it would be better to just call the eth_ method to pre-check the whole transaction
		if (m_order.isBuy() && !m_stablecoin.isRusd() ) {
			double approvedAmt = chain().busd().getAllowance( m_walletAddr, chain().params().rusdAddr() ); 
			require( 
					Util.isGtEq(approvedAmt, m_stablecoinAmt), 
					RefCode.INSUFFICIENT_ALLOWANCE,
					"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, m_stablecoinAmt); 
		}
		
	}

	/** The order was submitted. It may have been filled, maybe not. We must unwind the order from the
	 *  PositionTracker and submit a reverse order if necessary.
	 *  
	 *  This method changes all the main attributes of the order including price, size, and side
	 *  
	 *  */ 
	private void unwindOrder() {
		try {
			int conid = m_map.getRequiredInt("conid");

			// if no shares were filled, just remove the balances from the position tracker
			if (m_filledShares == 0) {
				out( "Unwinding order from PositionTracker"); 
				positionTracker.undo( conid, isBuy(), m_desiredQuantity, m_order.roundedQty() );
			}
			
			// if shares were filled, have to execute an opposing trade
			else {
				Contract contract = new Contract();
				contract.conid( conid);
				contract.exchange( m_main.getExchange( contract.conid() ) );
			
				m_order.orderType(OrderType.LMT); // this won't work off-hours
				m_order.flipSide();
				m_order.orderRef(m_uid + " unwind");
				m_order.roundedQty(  // use the PositionTracker to determine number of shares to buy or sell; it may be different from the original number if other orders have filled in between 
						positionTracker.buyOrSell( contract.conid(), m_order.isBuy(), m_desiredQuantity) );
				
				jlog( LogType.UNWIND_ORDER, m_order.getJsonLog(contract) );
				
				if (m_order.roundedQty() > 0 && !chain().params().autoFill() ) {
					
					// start w/ bid/ask and adjust price
					// this is tricky; too aggressive and we risk a bad fill; 
					// too conservative and we risk not filling at all
					Prices prices = m_stock.prices();
					double price = m_order.isBuy() ? prices.bid() * 1.01 : prices.ask() * .99;
					Util.require( price > 0, "Can't unwind, no market price");  // adjustment won't affect this 
					m_main.orderController().placeOrder(contract, m_order, null);
					
					// cancel the order; if it already filled, there's no harm done
					// we can't wait too long for this to fill because it will prevent us from
					// placing orders for the same contract on the opposite side
					Util.executeIn( 5000, () -> m_main.orderController().cancelOrder( m_order.orderId(), "", null) );
					
					// better would be to monitor the order and only cancel if not filled
				}
			}
		}
		catch( Exception e) {
			elog( LogType.UNWIND_ERROR, e);
			e.printStackTrace();
			alert( "Error occurred while unwinding order", e.getMessage() );
		}
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
			elog( LogType.ERROR_5, e);
			onFail(e.getMessage(), e.code() );
		}
		catch( Throwable e) {
			e.printStackTrace();
			elog( LogType.ERROR_6, e);
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
		order.put( "createdAt", m_createdAt);
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
	
	/** Return a message to the Frontend to be displayed to the user in a toast.
	 *  It is picked up by the live order system and then cleared so as not to
	 *  be displayed twice. Only the most recent message will be displayed. */
	public synchronized JsonObject getMessage() {
		try {
			return m_message != null 
					? Util.toJson( 
						"id", uid(),
						"type", "message",
						"text", m_message)
					: null;
		}
		finally {
			m_message = null;
		}
	}
	
	private String getWorkingOrderText() {
		return S.format( "%s %s %s for %s",
				m_order.action(), m_desiredQuantity, m_stock.symbol(), m_stablecoinAmt);
	}
	
	private String getCompletedOrderText() {
		return S.format( "%s %s %s for %s",
				isBuy() ? "Bought" : "Sold", m_desiredQuantity, m_stock.symbol(), m_stablecoinAmt);
	}

	@Override public void orderState(OrderState orderState) {
		// ignore this, we don't care
	}
	
	/** If we come here, it mean the order failed before it went to the LIVE state,
	 *  so no entry was made in the transactions table. Insert a placeholder into
	 *  the transactions table so we have a record of the order.
	 *  @param refCode could be null */
	@Override protected void postWrap(Enum refCode) {
		removeFromUserTokenMgr();
		
		m_main.queueSql( conn -> conn.insertJson("transactions", 
				Util.toJson( 
						"uid", m_uid,
						"wallet_public_key", m_walletAddr,
						"status", FireblocksStatus.DENIED,
						"ref_code", refCode,
						"chainid", chainIdIf()
//						"action", m_order.action() ); // enums gets quotes upon insert
//						"quantity", m_order.totalQty());
//						"symbol", m_stock.getSymbol() );
//						"conid", m_stock.getConid() );
//						"price", m_order.lmtPrice() );
						
						) ) );
	}

	/** This must get called when an order is completed, no matter how it completes, if
	 *  the order was added to the UserTokenMgr. It may/will be called multiple times, it's fine */
	private synchronized void removeFromUserTokenMgr() {
		if (m_sourceTokenQty > 0) {
			var srcTokAddress = getSourceToken().address();
			out( "Removing order from userTokenMgr %s %s %s", m_walletAddr, srcTokAddress, m_sourceTokenQty);
			UserTokenMgr.getUserToken( m_walletAddr, srcTokAddress).decrement(m_sourceTokenQty);
			m_sourceTokenQty = 0;
		}
	}

	static JsonArray dumpPositionTracker() {
		return positionTracker.dump();
	}

	private static final String buyConf = """
		<html>
		Your order on Reflection was filled!<p>
		<p>
		You bought %s shares of %s stock token for $%s.<p>
		<p>
		You paid with %s.<p>
		<p>
		We have purchased the associated stock and are holding it in reserve on your behalf.<p>
		<p>
		To view the stock token in your crypto wallet, click the "Add to Wallet" button on the
		Trade screen, or import this contract address: %s<p>
		<p>
		You can <a href="%s">view the transaction on the blockchain explorer</a><p>
		<p>
		If you have any questions or comments, feel free to reply to this email.<p>
		<p>
		Thank you!<p>
		<p>
		-The Reflection Team
		</html>""";

	private static final String sellConf = """
		<html>
		Your order on Reflection was filled!<p>
		<p>
		You sold %s shares of %s stock token for $%s.<p>
		<p>
		You received %s, the Reflection stablecoin.<p>
		<p>
		To view the RUSD in your your crypto wallet, import this contract address: %s<p>
		<p>
		You can <a href="%s">view the transaction on the blockchain explorer</a><p>
		<p>
		If you have any questions or comments, feel free to reply to this email.<p>
		<p>
		Thank you!<p>
		<p>
		-The Reflection Team
		</html>""";
}
// look at all the catch blocks, save message or stack trace
// you have to not log the cookie
// check logs for FB updates
// for log entry, you can trim the wallet
