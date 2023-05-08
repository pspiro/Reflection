package reflection;

import static reflection.Main.log;
import static reflection.Main.m_config;
import static reflection.Main.require;

import java.sql.SQLException;

import static reflection.Main.m_config;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Fireblocks;
import fireblocks.StockToken;
import reflection.MyTransaction.Stablecoin;
import tw.util.S;
import util.LogType;

public class OrderTransaction extends MyTransaction {
	private double desiredQuantity;
	private Stock m_stock;
	
	public OrderTransaction(Main main, HttpExchange exch) {
		super(main, exch);
 	}
	
	/** Msg received directly from Frontend via nginx */
	public void backendOrder() {
		wrap( () -> {
			require( "POST".equals(m_exchange.getRequestMethod() ), RefCode.INVALID_REQUEST, "order and check-order must be POST"); 
			parseMsg();
			order();
		});
    }

	private void order() throws Exception {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");
		m_stock = m_main.getStock(conid);  // throws exception if conid is invalid

		String side = action();
		require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");

		desiredQuantity = m_map.getRequiredDouble( "quantity");
		require( desiredQuantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");

		double price = m_map.getRequiredDouble( "tokenPrice");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double amt = price * desiredQuantity;
		double maxAmt = side == "buy" ? m_config.maxBuyAmt() : m_config.maxSellAmt();
		require( amt <= maxAmt, RefCode.ORDER_TOO_LARGE, "The total amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( amt), S.formatPrice( maxAmt) ); // this is displayed to user
		
		
		String wallet = m_map.getRequiredParam("wallet_public_key");
		require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid");
		
		// make sure user is signed in with SIWE and session is not expired
		// only trade and redeem messages need this
		validateCookie(wallet);
		
		// calculate order price
		double prePrice;
		if (side == "buy") {
			prePrice = price - price * m_config.minBuySpread();
		}
		else {
			prePrice = price + price * m_config.minSellSpread();
		}
		double orderPrice = Util.round( prePrice);  // round to two decimals
		
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		Order order = new Order();
		order.action( side == "buy" ? Action.BUY : Action.SELL);
		order.totalQuantity( desiredQuantity);
		order.lmtPrice( orderPrice);
		order.tif( TimeInForce.IOC);
		order.allOrNone(true);  // all or none, we don't want partial fills
		order.transmit( true);
		order.outsideRth( true);
		order.walletAddr( wallet);
		
		// request contract details (prints to stdout)
		insideAnyHours( contract, inside -> {
			require( inside, RefCode.EXCHANGE_CLOSED, exchangeIsClosed);

			// check that we have prices and that they are within bounds;
			// do this after checking trading hours because that would
			// explain why there are no prices which should never happen otherwise
			Prices prices = m_main.getPrices( contract.conid() );
			prices.checkOrderPrice( order, orderPrice, m_config);
			
			// ***check that the prices are pretty recent; if they are stale, and order is < .5, we will fill the order with a bad price. pas
			// * or check that ANY price is pretty recent, to we know prices are updating
			
			log( LogType.ORDER, order.getOrderLog(contract) );

			// *if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas
			if (order.roundedQty() == 0) {
				respondToOrder(order, 0, false, OrderStatus.Filled);
			}
			// AUTO-FILL - for testing only
			else if (m_config.autoFill() ) {
				require( !m_config.isProduction(), RefCode.REJECTED, "Cannot use auto-fill in production" );
				log( LogType.AUTO_FILL, "id=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
						order.orderId(), order.action(), order.totalQty(), order.totalQty(), order.lmtPrice(),
						m_config.commission(), 0, "");
				respondToOrder( order, order.totalQuantity(), false, OrderStatus.Filled); // you might want to sometimes pass false here when testing
			}
			else {
				submitOrder(  contract, order);
			}
		});
	}


	private void submitOrder( Contract contract, Order order) throws Exception {
		ModifiableDecimal shares = new ModifiableDecimal();

		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

				wrap( () -> {
					S.out( "  order status  id=%s  status=%s", order.orderId(), status);

					// save the number of shares filled
					shares.value( filled.toDouble() );
					//shares.value = filled.toDouble() - 1;  // to test partial fills

					// better is: if canceled w/ no shares filled, let it go to handle() below

					if (status.isComplete() ) {
						respondToOrder( order, shares.value(), false, status);
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

		log( LogType.SUBMIT, "wallet=%s  orderid=%s", order.walletAddr(), order.orderId() );

		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( m_config.orderTimeout(), () -> onTimeout( order, shares.value(), OrderStatus.Unknown) );
	}
	
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  In the case where order qty < .5 and we didn't submit an order,
	 *  orderStatus will be Filled. */
	private synchronized void respondToOrder(Order order, double filledShares, boolean timeout, OrderStatus status) throws Exception {
		if (m_responded) {
			return;    // this happens when the timeout occurs after an order is filled, which is normal
		}

		// no shares filled and order size >= .5?
		if (filledShares == 0 && status != OrderStatus.Filled) {  // Filled status w/ zero shares means order size was < .5
			String msg = timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.";
			respondFull( 
					Util.toJsonMsg( code, RefCode.REJECTED, message, msg),
					400,
					null);
			log( LogType.REJECTED, "id=%s  orderQty=%s  orderPrc=%s  reason=%s",
					order.orderId(), order.totalQty(), order.lmtPrice(), msg);
			return;
		}

		double stockTokenQty;  // quantity of stock tokens to swap
		LogType logType;  // fill or partial fill
		RefCode refCode;  // fill or partial fill

		// for a filled order, the (order size - filled size) should always be <= .5
		// if > .5, then it was a partial fill and we will use the filled size instead of the order size
		// this way we always have max .5 shares difference between stock pos and token pos (for a single order)
		if (order.totalQuantity() - filledShares > .5001) {
			// this should never happen since we set all-or-none on the orders
			stockTokenQty = filledShares;
			logType = LogType.PARTIAL_FILL;
			refCode = RefCode.PARTIAL_FILL;
		}
		else {
			stockTokenQty = order.totalQuantity();
			logType = LogType.FILLED;
			refCode = RefCode.OK;
		}

		double tds = 0;     // the tds tax paid by Indian residents
		String hash = "";   // the blockchain hashcode

		if (fireblocks() ) {
			try {
				String id;
				S.out( "Starting Fireblocks protocol");
				
				// for testing
				if (m_map.getBool("fail") ) {
					throw new Exception("Blockchain transaction failed intentially during testing"); 
				}

				double stablecoinAmt = m_map.getDouble("price");
				
				// buy
				if (order.action() == Action.BUY) {
					
					// buy with RUSD?
					if (m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.RUSD) {
						id = m_config.rusd().buyStockWithRusd(
								order.walletAddr(), 
								stablecoinAmt,
								newStockToken(),
								stockTokenQty
						);
					}
					
					// buy with BUSD
					else {
						id = m_config.rusd().buyStock(
								order.walletAddr(),
								m_config.busd(),
								stablecoinAmt,
								newStockToken(), 
								stockTokenQty
						);
					}
				}
				
				// sell
				else {
					id = m_config.rusd().sellStockForRusd(
							order.walletAddr(),
							stablecoinAmt,
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
				log( LogType.ERROR, "Fireblocks failed for order %s - %s", order.orderId(), e.getMessage() );

				// try to figure out why the order failed
				
				// fireblocks has failed; try to determine why and respond() to client
				wrap( () -> {
					double totalOrderAmt = m_map.getRequiredDouble("price");  // including commission, very poorly named field

					// confirm that the user has enough stablecoin or stock token in their wallet
					if (order.isBuy() ) {
						double balance = stablecoin().getPosition( order.walletAddr() );
						require( Util.isGtEq(balance, totalOrderAmt), 
								RefCode.INSUFFICIENT_FUNDS,
								"The stablecoin balance (%s) is less than the total order amount (%s)", 
								balance, totalOrderAmt);
					}
					else {
						double balance = newStockToken().getPosition( order.walletAddr() );
						require( Util.isGtEq(balance, desiredQuantity), 
								RefCode.INSUFFICIENT_FUNDS,
								"The stock token balance (%s) is less than the order quantity (%s)", 
								balance, desiredQuantity);
					}
	
					// if buying with BUSD, confirm the "approved" amount of BUSD is >= order amt
					if (order.isBuy() && m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.USDC) {
						double approvedAmt = m_config.busd().getAllowance( order.walletAddr(), m_config.rusdAddr() ); 
						require( Util.isGtEq(approvedAmt, totalOrderAmt), RefCode.INSUFFICIENT_ALLOWANCE,
								"The approved amount of stablecoin (%s) is insufficient for the order amount (%s)", approvedAmt, totalOrderAmt); 
					}

					// we don't know why it failed, so throw the Fireblocks error
					throw e;
				});
					
				// FB has failed, we have responded, now unwind the order
				unwindOrder(order, filledShares);
				
				return;
			}
		}

		respond( code, refCode, "filled", stockTokenQty);

		log( logType, "id=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s",
				order.orderId(), order.action(), order.totalQty(),
				S.fmt4(filledShares), order.lmtPrice(),
				m_config.commission(), tds, hash);
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
					"wallet_public_key", order.walletAddr(),
					"symbol", m_stock.getSymbol(),
					"conid", m_stock.getConid(),
					"action", order.action().toString(),
					"quantity", order.totalQty(),
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

	private synchronized void onTimeout(Order order, double filledShares, OrderStatus status) throws Exception {
		// this could happen if our timeout is lower than the timeout of the IOC order,
		// which should never be the case
		if (!m_responded) {
			log( LogType.ORDER_TIMEOUT, "id=%s   order timed out with %s shares filled and status %s", 
					order.orderId(), filledShares, status);

			// if order is still live, cancel the order
			if (!status.isComplete() && !status.isCanceled() ) {
				S.out( "Canceling order %s on timeout", order.orderId() );
				m_main.orderController().cancelOrder( order.orderId(), "", null);
			}

			respondToOrder( order, filledShares, true, status);
		}
	}
	
	/** The order was filled, but the blockchain transaction failed, so we must unwind the order. 
	 * @param filledShares */
	private void unwindOrder(Order order, double filledShares) {
		try {
			// don't unwind order in auto-fill mode which is for testing only
			if (m_config.autoFill() ) {
				S.out( "Not unwinding order in auto-fill mode");
				return;
			}

			String body = String.format( "The blockchain transaction failed and the order should be unwound:  wallet=%s  orderid=%s",
					order.walletAddr(), order.orderId() );
			alert( "UNWIND ORDER", body);
			
			Contract contract = new Contract();
			contract.conid( m_map.getRequiredInt("conid") );
			contract.exchange( m_main.getExchange( contract.conid() ) );
			
			order.flipSide();
			order.orderId(0);
			order.orderType(OrderType.MKT);
			
			// this should never be the case since the orders are AON, but just in case that changes...
			if (filledShares < order.totalQuantity() ) {
				S.out( "WARNING: filled shared was less that total order qty when unwinding order"); 
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
	
	

}
