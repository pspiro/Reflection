package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import java.util.Vector;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.StockToken;
import reflection.LiveOrder.FireblocksStatus;
import tw.util.S;
import util.LogType;

public class OrderTransaction extends LiveOrderTransaction {
	private double m_desiredQuantity;  // same as Order.m_totalQuantity
	private Stock m_stock;
	private double m_stablecoinAmt;
	private double m_tds;
	private boolean m_respondedToOrder;
	private String m_walletAddr;
	private LiveOrder m_liveOrder;  // you could use existing Order class instead
	
	public OrderTransaction(Main main, HttpExchange exch) {
		super(main, exch);
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

		Order order = new Order();
		order.action( side == "buy" ? Action.BUY : Action.SELL);
		order.totalQuantity( m_desiredQuantity);
		order.lmtPrice( orderPrice);
		order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
		order.allOrNone(true);  // all or none, we don't want partial fills
		order.transmit( true);
		order.outsideRth( true);

		// check TDS calculation
		m_tds = m_map.getDouble("tds");
		double myTds = order.isBuy() ? 0 : (preCommAmt - m_config.commission() ) * .01;
		// fix this -> require( Util.isEq( m_tds, myTds, .001), RefCode.INVALID_REQUEST, "TDS of %s does not match calculated amount of %s", m_tds, myTds); 
		
		m_stablecoinAmt = m_map.getRequiredDouble("price");
		
		double myStablecoinAmt = order.isBuy()
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
		prices.checkOrderPrice( order, orderPrice, m_config);
		
		// ***check that the prices are pretty recent; if they are stale, and order is < .5, we will fill the order with a bad price. pas
		// * or check that ANY price is pretty recent, to we know prices are updating
		
		log( LogType.REC_ORDER, order.getOrderLog(contract, m_walletAddr) );
		
		m_liveOrder = new LiveOrder(
				side, 
				S.format("%s %s %s for $%s", cap(side), m_desiredQuantity, m_stock.getSymbol(), m_stablecoinAmt),
				m_uid); 
		
		respond( code, RefCode.OK, "id", m_liveOrder.uid() );
		
		// now it is up to the live order system to report success or failure
		walletLiveOrders().add( m_liveOrder);

		shrinkWrap( () -> {
			// *if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas
			if (order.roundedQty() == 0) {
				order.status(OrderStatus.Filled);
				onIBOrderCompleted(order, 0, false);
			}
			// AUTO-FILL - for testing only
			else if (m_config.autoFill() ) {
				require( !m_config.isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
				log( LogType.AUTO_FILL, "id=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
						order.orderId(), order.action(), order.totalQty(), order.totalQty(), order.lmtPrice(),
						m_config.commission(), 0, "");
				order.status(OrderStatus.Filled);
				onIBOrderCompleted( order, order.totalQuantity(), false ); // you might want to sometimes pass false here when testing
			}
			else {
				submitOrder(  contract, order);
			}
		});
	}

	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require() and no wrap(),
	 *  only shrinkWrap()  */
	private void submitOrder( Contract contract, Order order) throws Exception {
		ModifiableDecimal shares = new ModifiableDecimal();

		// place order for rounded quantity
		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

				shrinkWrap( () -> {
					order.status(status);
					out( "  order status  id=%s  status=%s", order.orderId(), status);

					// save the number of shares filled
					shares.value( filled.toDouble() );
					//shares.value = filled.toDouble() - 1;  // to test partial fills

					// better is: if canceled w/ no shares filled, let it go to handle() below

					if (status.isComplete() ) {
						order.permId(permId);
						onIBOrderCompleted( order, shares.value(), false);
					}
				});
			}

			@Override public void handle(int errorCode, String errorMsg) {
				shrinkWrap( () -> {
					log( LogType.ORDER_ERR, "id=%s  errorCode=%s  errorMsg=%s", order.orderId(), errorCode, errorMsg);

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

		log( LogType.SUBMITTED, order.getOrderLog(contract, m_walletAddr) );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( m_config.orderTimeout(), () -> onOrderTimeout( order, shares.value() ) );
	}
	
	/** NOTE: You MUST call onIBOrderCompleted() once you come in here, so no require()
	 *  this is wrapped() but it must call onIBOrderCompleted() so cannot throw an exception  */
	private synchronized void onOrderTimeout(Order order, double filledShares) {
		shrinkWrap( () -> {
			if (!m_respondedToOrder) {
				// this will happen if our timeout is lower than the timeout of the IOC order
				log( LogType.ORDER_TIMEOUT, "id=%s   order timed out with %s shares filled and status %s", 
						order.orderId(), filledShares, order.status() );

				// if order is still live, cancel the order; don't let an error here disrupt processing
				if (!order.status().isComplete() && !order.status().isCanceled() ) {
					try {
						out( "Canceling order %s on timeout", order.orderId() );
						m_main.orderController().cancelOrder( order.orderId(), "", null);
					}
					catch( Exception e) {
						e.printStackTrace();
					}
				}
				onIBOrderCompleted( order, filledShares, true);
			}
		});
	}

	/** We cannot throw an exception. We have already called respond(), we must now be sure to update the live order */
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  This call is shrinkWrapped()
	 *  In the case where order qty < .5 and we didn't submit an order, orderStatus will be Filled.
	 *  You must (a) make a log entry and (b) either call m_liveOrder.filled() or throw an exception */ 
	private synchronized void onIBOrderCompleted(Order order, double filledShares, boolean timeout) throws Exception {
		if (m_respondedToOrder) {
			return;    // should never happen, but just to be safe
		}
		m_respondedToOrder = true;

		require(
				filledShares > 0 || order.status() == OrderStatus.Filled,
				timeout ? RefCode.TIMED_OUT : RefCode.UNKNOWN,
				timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.");
		

		// ----- the order has been filled or partially filled -----

		
		double stockTokenQty;  // quantity of stock tokens to swap
		LogType logType;  // fill or partial fill

		// for a filled order, the (order size - filled size) should always be <= .5
		// if > .5, then it was a partial fill and we will use the filled size instead of the order size
		// this way we always have max .5 shares difference between stock pos and token pos (for a single order)
		if (order.totalQuantity() - filledShares > .5001) {
			stockTokenQty = filledShares;
			logType = LogType.PARTIAL_FILL;  // this should never happen since we set all-or-none on the orders
		}
		else {
			stockTokenQty = order.totalQuantity();
			logType = LogType.FILLED;
		}
		
		log( logType, "orderId=%s  filledShares=%s", order.orderId(), filledShares);
		
		if (fireblocks() ) {
			m_liveOrder.updateFrom(FireblocksStatus.STOCK_ORDER_FILLED);
			processFireblocks(order, stockTokenQty, filledShares);
		}
		else {
			m_liveOrder.filled();
		}
	}
	
	/** Call to this method is shrinkWrapped() */
	private void processFireblocks(Order order, double stockTokenQty, double filledShares) throws Exception {
		try {			
			// for testing
			if (m_map.getBool("fail") ) {
				throw new Exception("Blockchain transaction failed intentially during testing"); 
			}

			out( "Starting Fireblocks protocol");

			String id;

			// buy
			if (order.action() == Action.BUY) {
				
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
			
			allLiveOrders.put(id, m_liveOrder);
			
			// if we don't make it to here, it means there was an exception which will be picked up
			// by shrinkWrap() and the live order will be failed()
		}
		catch( Exception e) {  // for FB errors, we don't need to print a stack trace; maybe throw RefException for those
			if (!(e instanceof RefException) ) {
				e.printStackTrace();
			}
			
			log( LogType.ERROR, "orderId=%s  %s", order.orderId(), e.getMessage() ); 

			// unwind the order first and foremost
			unwindOrder(order, filledShares);
			
			// now throw an exception so the LiveOrder will get updated with the error text

			// fireblocks has failed; try to determine why
			
			// confirm that the user has enough stablecoin or stock token in their wallet
			requireSufficientStablecoin(order); // check this again; it could have changes since the order was placed

			// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
			if (order.isBuy() && m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC) {
				double totalOrderAmt = m_map.getRequiredDouble("price");  // including commission, very poorly named field
				double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() ); 
				require( 
						Util.isGtEq(approvedAmt, totalOrderAmt), 
						RefCode.INSUFFICIENT_ALLOWANCE,
						"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, totalOrderAmt); 
			}

			// we don't know why it failed, so throw the Fireblocks error
			throw e;
		}
	}

	/** Confirm that the user has enough stablecoin or stock token in their wallet.
	 *  This could be called before or after submitting the stock order */
	private void requireSufficientStablecoin(Order order) throws Exception {
		if (order.isBuy() ) {
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
	private void unwindOrder(Order order, double filledShares) {
		try {
			// don't unwind order in auto-fill mode which is for testing only
			if (m_config.autoFill() ) {
				out( "Not unwinding order in auto-fill mode");
				return;
			}

			String body = String.format( "The blockchain transaction failed and the order should be unwound:  wallet=%s  orderid=%s",
					m_walletAddr, order.orderId() );
			alert( "UNWIND ORDER", body);
			
			Contract contract = new Contract();
			contract.conid( m_map.getRequiredInt("conid") );
			contract.exchange( m_main.getExchange( contract.conid() ) );
			
			order.flipSide();
			order.orderId(0);
			order.orderType(OrderType.MKT);
			
			// this should never be the case since the orders are AON, but just in case that changes...
			if (filledShares < order.totalQuantity() ) {
				out( "WARNING: filled shared was less that total order qty when unwinding order"); 
				order.totalQuantity(filledShares);
			}
			
			m_main.orderController().placeOrModifyOrder(contract, order, null);
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

	private Vector<LiveOrder> walletLiveOrders() {
		return Util.getOrCreate(liveOrders, m_walletAddr.toLowerCase(), () -> new Vector<LiveOrder>() );
	}

	/** Return Buy or Sell starting w/ upper case */
	private static String cap(String side) {
		return "buy".equals(side) ? "Buy" : "Sell";
	}
	
	private void shrinkWrap(ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			out( e);
			log( LogType.ERROR, e.toString() );
			m_liveOrder.failed(e); // you have to sync this. pas
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			m_liveOrder.failed(e); // you have to sync this. pas
		}
	}
}
