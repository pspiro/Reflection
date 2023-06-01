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

import fireblocks.Fireblocks;
import fireblocks.StockToken;
import tw.util.S;
import util.LogType;

public class OrderTransaction extends MyTransaction {
	private double m_desiredQuantity;  // same as Order.m_totalQuantity
	private Stock m_stock;
	private double m_stablecoinAmt;
	private double m_tds;
	private boolean m_respondedToOrder;
	private String m_walletAddr; // you could move this to OrderTransaction if desired, it's not really part of the order
	private LiveOrder m_liveOrder;  // you could use existing Order class instead
	
	public OrderTransaction(Main main, HttpExchange exch) {
		super(main, exch, "ORD");
 	}
	
	/** Msg received directly from Frontend via nginx */
	public void backendOrder() {
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
		
		// request contract details (prints to stdout)
		require( m_main.m_tradingHours.insideAnyHours( m_stock.getBool("is24hour"), m_map.get("simtime")), RefCode.EXCHANGE_CLOSED, exchangeIsClosed);

		// check that we have prices and that they are within bounds;
		// do this after checking trading hours because that would
		// explain why there are no prices which should never happen otherwise
		Prices prices = m_main.getStock(conid).prices();
		prices.checkOrderPrice( order, orderPrice, m_config);
		
		// ***check that the prices are pretty recent; if they are stale, and order is < .5, we will fill the order with a bad price. pas
		// * or check that ANY price is pretty recent, to we know prices are updating
		
		log( LogType.ORDER, order.getOrderLog(contract, m_walletAddr) );
		
		m_liveOrder = new LiveOrder(
				side, 
				S.format("%s %s %s for $%s", cap(side), m_desiredQuantity, m_stock.getSymbol(), m_stablecoinAmt) ); 
		
		// now it is up to the live order system to report success or failure
		walletLiveOrders().add( m_liveOrder);

		respond( code, RefCode.OK, "id", m_liveOrder.id() );
		
		// *if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas
		if (order.roundedQty() == 0) {
			order.status(OrderStatus.Filled);
			respondToOrder(order, 0, false);
		}
		// AUTO-FILL - for testing only
		else if (m_config.autoFill() ) {
			require( !m_config.isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
			log( LogType.AUTO_FILL, "id=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
					order.orderId(), order.action(), order.totalQty(), order.totalQty(), order.lmtPrice(),
					m_config.commission(), 0, "");
			order.status(OrderStatus.Filled);
			respondToOrder( order, order.totalQuantity(), false ); // you might want to sometimes pass false here when testing
		}
		else {
			submitOrder(  contract, order);
		}
	}

	/** NOTE: You MUST call respondToOrder() once you come in here, so no require() */
	private void submitOrder( Contract contract, Order order) throws Exception {
		ModifiableDecimal shares = new ModifiableDecimal();

		// place order for rounded quantity
		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

				wrap( () -> {
					order.status(status);
					out( "  order status  id=%s  status=%s", order.orderId(), status);

					// save the number of shares filled
					shares.value( filled.toDouble() );
					//shares.value = filled.toDouble() - 1;  // to test partial fills

					// better is: if canceled w/ no shares filled, let it go to handle() below

					if (status.isComplete() ) {
						order.permId(permId);
						respondToOrder( order, shares.value(), false);
					}
				});
			}

			@Override public void handle(int errorCode, String errorMsg) {
				wrap( () -> {
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

		log( LogType.SUBMIT, "wallet=%s  orderid=%s", m_walletAddr, order.orderId() );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( m_config.orderTimeout(), () -> onOrderTimeout( order, shares.value() ) );
	}
	
	/** NOTE: You MUST call respondToOrder() once you come in here, so no require() */
	private synchronized void onOrderTimeout(Order order, double filledShares) throws Exception {
		// this could happen if our timeout is lower than the timeout of the IOC order,
		// which should never be the case
		if (!m_respondedToOrder) {
			log( LogType.ORDER_TIMEOUT, "id=%s   order timed out with %s shares filled and status %s", 
					order.orderId(), filledShares, order.status() );

			// if order is still live, cancel the order
			if (!order.status().isComplete() && !order.status().isCanceled() ) {
				out( "Canceling order %s on timeout", order.orderId() );
				m_main.orderController().cancelOrder( order.orderId(), "", null);
			}

			respondToOrder( order, filledShares, true);
		}
	}
	
	/** We cannot throw an exception. We have already called respond(), we must now be sure to update the live order */
	private synchronized void respondToOrder(Order order, double filledShares, boolean timeout) {
		try {
			respondToOrder_(order, filledShares, timeout);
		}
		catch( Exception e) {
			m_liveOrder.failed(e); // you have to sync this. pas
			
			log( LogType.FAILED, "id=%s  permId=%s  orderQty=%s  orderPrc=%s  reason=%s",
					order.orderId(), order.permId(), order.totalQty(), order.lmtPrice(), e.getMessage() );
		}
	}
	
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  In the case where order qty < .5 and we didn't submit an order, orderStatus will be Filled.
	 *  You must (a) make a log entry and (b) either call m_liveOrder.filled() or throw an exception */ 
	private void respondToOrder_(Order order, double filledShares, boolean timeout) throws Exception {
		if (m_respondedToOrder) {
			return;    // this happens when the timeout occurs after an order is filled, which is normal
		}
		m_respondedToOrder = true;

		require(
				filledShares > 0 || order.status() == OrderStatus.Filled,
				timeout ? RefCode.TIMED_OUT : RefCode.UNKNOWN,
				timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.");				

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

		String hash = "";   // the blockchain hashcode

		if (fireblocks() ) {
			try {
				String id;
				out( "Starting Fireblocks protocol");
				
				// for testing
				if (m_map.getBool("fail") ) {
					throw new Exception("Blockchain transaction failed intentially during testing"); 
				}

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

				// it would be better if we could send back the response in two blocks, one
				// when the order fills and one when the blockchain transaction is completed

				// wait for the transaction to be signed
				// this won't be good if we have multiple orders pending since each one is
				// polling every one second; either put them in a queue or use the Fireblocks
				// callback mechanism
				hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
				
				// insert transaction into database
				insertCryptoTrans(order, hash);
				
				log( LogType.ORDER, "Order %s completed Fireblocks transaction with hash %s", order.orderId(), hash);
			}
			catch( Exception e) {  // for FB errors, we don't need to print a stack trace; maybe throw RefException for those
				e.printStackTrace();

				// FB has failed, unwind the order
				unwindOrder(order, filledShares);

				// fireblocks has failed; try to determine why
				double totalOrderAmt = m_map.getRequiredDouble("price");  // including commission, very poorly named field
				
				// confirm that the user has enough stablecoin or stock token in their wallet
				requireSufficientStablecoin(order); // check this again; it could have changes since the order was placed

				// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
				if (order.isBuy() && m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC) {
					double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() ); 
					require( Util.isGtEq(approvedAmt, totalOrderAmt), RefCode.INSUFFICIENT_ALLOWANCE,
							"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, totalOrderAmt); 
				}

				// we don't know why it failed, so throw the Fireblocks error
				throw e;
			}
		}

		m_liveOrder.filled(stockTokenQty);  // Fireblocks either succeeded or we skipped it

		log( logType, "id=%s  action=%s  desiredQty=%s  roundedQty=%s  filled=%s  stockTokenQty=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
				order.orderId(), order.action(), order.totalQty(), order.roundedQty(),
				S.fmt4(filledShares), stockTokenQty, order.lmtPrice(),
				m_config.commission(), m_tds, hash);
	}
	
	/** Confirm that the user has enough stablecoin or stock token in their wallet */
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

	/** Insert record into crypto_transactions table.
	 *  An error here should not disrupt the flow, we should just log it and move on */
	private void insertCryptoTrans(Order order, String transId) {
		try {
			m_config.sqlConnection( conn -> 
				conn.insertPairs("crypto_transactions",
					"crypto_transaction_id", transId,
					"timestamp", System.currentTimeMillis() / 1000, // why do we need this and also the other dates?
					"wallet_public_key", m_walletAddr,
					"symbol", m_stock.getSymbol(),
					"conid", m_stock.getConid(),
					"action", order.action().toString(),
					"quantity", order.totalQty(),  // same as m_desiredQuantity
					"price", order.lmtPrice(),
					"commission", m_config.commission(), // not so good, we should get it from the order. pas
					"spread", order.isBuy() ? m_config.buySpread() : m_config.sellSpread(),
					//"status",
					//"ip_address",
					//"city",
					//"country",
					// "crypto_id",
					"currency", m_map.getEnumParam("currency", Stablecoin.values() ).toString()
				)
			);
		}
		catch (Exception e) {
			log( LogType.ERROR, "Error inserting record into crypto_transactions table: " + e.getMessage() );
			e.printStackTrace();
		}
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
	

}


//YOU ARE WORKING ON TWO THINGS:
//	1. ORDER TIMEOUT COULD CAUSE FIREBLOCKS TO HAPPEN TWICE
//	2. YOU CAN'T TIE UP THE TWS API THREAD EVER WHILE PROCESSING A RESPONSE, IN THIS CASE 
//	   ORDER HOURS; SOLVE IT BY RE-DESIGNING THE ORDER HOURS IN THIS PARTICULAR CASE, BUT BE CAREFUL