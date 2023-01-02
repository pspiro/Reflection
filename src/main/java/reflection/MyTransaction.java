package reflection;

import static reflection.Main.log;
import static reflection.Main.require;
import static reflection.Util.inside;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Deploy;
import fireblocks.Rusd;
import json.StringJson;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import tw.util.S;
import util.LogType;

class MyTransaction {
	enum MsgType {
		checkHours, checkOrder, disconnect, dump, getAllPrices, getAllStocks, getConfig, getConnectionStatus, getDescription, getPrice, order, orderFb, pullBackendConfig, pullFaq, pushBackendConfig, pushFaq, refreshConfig, refreshStocks, terminate;
		
		public static String allValues() {
			return Arrays.asList( values() ).toString();
		}
	}
	

	static double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	static final String code = "code";
	static final String text = "text";
	
	private Main m_main;
	private HttpExchange m_exchange;
	private boolean m_responded;  // only respond once per transaction
	private ParamMap m_map = new ParamMap();

	MyTransaction( Main main, HttpExchange exchange) {
		m_main = main;
		m_exchange = exchange; 
	}

	void handle() {
		wrap( () -> handle2() );
	}
	
	// you could encapsulate all these methods in MyExchange

	void handle2() throws Exception {
		String uri = m_exchange.getRequestURI().toString().toLowerCase();
		require( uri.length() < 4000, RefCode.UNKNOWN, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
			S.out( "Received GET request %s %s", uri, m_exchange.getHttpContext().getPath() ); 
			// get right side of ? in URL
			String[] parts = uri.split("\\?");
			require( parts.length ==2, RefCode.INVALID_REQUEST, "No request present. Valid requests are " + MsgType.allValues() );

			// build map of tag/value, expecting tag=value&tag=value
			String[] params = parts[1].split( "&");
			//map.parseJson( )
			for (String param : params) {
				String[] pair = param.split( "=");
				require( pair.length == 2, RefCode.INVALID_REQUEST, "Tag/value format is incorrect");
				m_map.put( pair[0], pair[1]);
			}
		}
		
		// POST request
		else {
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );
	            
				JSONParser parser = new JSONParser();
	            JSONObject jsonObject = (JSONObject)parser.parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)
	            
	            for (Object key : jsonObject.keySet() ) {
	            	Object value = jsonObject.get(key);
	            	require( key instanceof String, RefCode.INVALID_REQUEST, "Invalid JSON, key is not a string");
	            	
	            	if (value != null) {
	            		m_map.put( (String)key, value.toString() );
	            	}
	            }

	            S.out( "Received POST request " + m_map.toString() );
			}
			catch( RefException e) {  // catch the above require() call
				throw e; 
			}
			catch( ParseException e) {   // this exception does not set the exception message text
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.toString() );  
			}
			catch( Exception e) {
				e.printStackTrace(); // should never happen
				throw new RefException( RefCode.INVALID_REQUEST, "Error parsing json - " + e.getMessage() ); // no point returning the message text because  
			}
		}

		MsgType msgType = m_map.getEnumParam( "msg", MsgType.values() );

		switch (msgType) { // this could be switched to polymorphism if desired
			case getPrice:
				getPrice();
				break;
			case getAllPrices:
				getAllPrices();
				break;
			case order:
				order( false, false);
				break;
			case orderFb:
				order( false, true);
				break;
			case checkOrder:
				order( true, false);
				break;
			case checkHours:
				checkHours();
				break;
			case getDescription:
				getDescription();
				break;
			case getAllStocks:
				getAllStocks();
				break;
			case refreshStocks:
				refreshStocks();
				break;
			case getConfig:
				getConfig();
				break;
			case refreshConfig:
				refreshConfig();
				break;
			case getConnectionStatus:
				getConnStatus();
				break;
			case pushBackendConfig:
				pushBackendConfig();
				break;
			case pullBackendConfig:
				pullBackendConfig();
				break;
			case pullFaq:
				pullFaq();
				break;
			case pushFaq:
				pushFaq();
				break;
			case terminate:
				terminate();
				break;
			case disconnect:
				disconnect();
				break;
			case dump:
				m_main.dump();
				respond( code, RefCode.OK);
				break;
		}
	}
	
	/** Simulate disconnect to test reconnect */
	private void disconnect() {
		S.out( "simulating disconnecting");
		m_main.orderConnMgr().disconnect();
		respond( code, RefCode.OK);
	}

	private void terminate() {
		log( LogType.TERMINATE, "");
		System.exit( 0);
	}

	/** Top-level message handler */ 
	private void pushFaq() throws Exception {
		S.out( "Pushing FAQ");
		Main.m_config.pushFaq( Main.m_database);
		respond( code, RefCode.OK);
	}

	/** Top-level message handler */ 
	private void pullFaq() throws Exception {
		S.out( "Pulling FAQ");
		Main.m_config.pullFaq( Main.m_database);
		respond( code, RefCode.OK);
	}

	/** Top-level message handler */ 
	private void pushBackendConfig() throws Exception {
		S.out( "Pushing backend config from google sheet to database");
		Main.m_config.pushBackendConfig( Main.m_database);
		respond( code, RefCode.OK);
	}

	/** Top-level message handler */ 
	private void pullBackendConfig() throws Exception {
		S.out( "Pulling backend config from database to google sheet");
		Main.m_config.pullBackendConfig( Main.m_database);
		respond( code, RefCode.OK);
	}

	/** Top-level message handler */
	private void getConnStatus() {
		S.out( "Sending connection status");
		respond( "orderConnectedToTWS", m_main.orderController().isConnected(), 
				 "orderConnectedToBroker", m_main.orderConnMgr().ibConnection() );
	}

	/** Top-level message handler */ 
	void getConfig() throws Exception {
		S.out( "Sending config");
		respond( Main.m_config.toJson() );
	}
	
	/** Top-level message handler */ 
	void refreshConfig() throws Exception {
		S.out( "Refreshing config from google sheet");
		Main.m_config.readFromSpreadsheet(m_main.tabName() );
		respond( Main.m_config.toJson() );
	}
	
	/** Top-level message handler */ 
	void refreshStocks() throws Exception {
		S.out( "Refreshing stock list from google sheet");
		m_main.refreshStockList();
		respond( code, RefCode.OK);
	}
	
	/** Top-level message handler */
	private void getAllStocks() throws RefException {
		require( !m_main.stocks().isEmpty(), RefCode.UNKNOWN, "We don't have the list of stocks");

		S.out( "Returning all stocks");
		respond( new Json( m_main.stocks() ) );
	}
	
	/** Top-level method; used for admin purposes only, to get the conid */
	private void getDescription() throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		S.out( "Returning stock description");
		Contract contract = new Contract();
		contract.secType( SecType.STK);
		contract.symbol( m_map.getRequiredParam("symbol").replace( '_', ' ') );
		contract.exchange( m_map.getParam("exchange") );
		contract.currency( m_map.getParam("currency") );

		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( list.size() > 0, RefCode.UNKNOWN, "No such stock");
				
				JSONArray whole = new JSONArray();
				
				for (ContractDetails deets : list) {
					JSONObject tradingHours = new JSONObject();
					tradingHours.put( "tradingHours", deets.tradingHours() );
					tradingHours.put( "liquidHours", deets.liquidHours() );
					tradingHours.put( "timeZone", deets.timeZoneId() );

					JSONObject obj = new JSONObject();
					obj.put( "symbol", deets.contract().symbol() );
					obj.put( "conid", deets.conid() );
					obj.put( "exchange", deets.contract().exchange() );
					obj.put( "prim_exchange", deets.contract().primaryExch() );
					obj.put( "currency", deets.contract().currency() );
					obj.put( "name", deets.longName() );
					obj.put( "tradingHours", tradingHours);

					whole.add( obj);
				}

				respond( new Json( whole).fmtArray() );
			});
		});

		setTimer( Main.m_config.timeout(), () -> timedOut( "getDescription timed out") );
	}

	/** Top-level method. */
	private void checkHours() throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		
		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "Param 'conid' must be positive integer");

		S.out( "Returning trading hours for %s", conid);
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		m_main.orderController().reqContractDetails(contract, list -> processHours( conid, list) );
		
		setTimer( Main.m_config.timeout(), () -> timedOut( "checkHours timed out") );
	}
	
	private void processHours(int conid, List<ContractDetails> list) {
		wrap( () -> {
			require( !list.isEmpty(), RefCode.NO_SUCH_STOCK, "No contract details found for conid %s", conid);
			
			ContractDetails deets = list.get(0);

			if (inside( deets.conid(), deets.liquidHours(), deets.timeZoneId() ) ) {
				respond( "hours", "liquid");
			}
			else if (inside( deets.conid(), deets.tradingHours(), deets.timeZoneId() ) ) {
				respond( "hours", "illiquid");
			}
			else {
				respond( "hours", "closed");
			}
		});
	}
	
	/** Top-level method. */
	// remove this. pas
	private void getPrice() throws RefException {
		int conid = m_map.getRequiredInt( "conid");
		
		Prices prices = getPrices( conid);
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		S.out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.bid(), prices.ask(), conid);
		respond( prices.toJson(conid) );
	}

	/** This returns json tags of bid/ask but it might be returning other prices if bid/ask is not available. */
	private Prices getPrices(int conid) {
		Map<String, String> ps = m_main.m_jedis.hgetAll( String.valueOf(conid) );
		return new Prices( ps);
	}
	
	/** Used to query prices from Redis. */
	static class PriceQuery {
		private int conid;
		private Response<Map<String, String>> res;

		public PriceQuery(Pipeline p, int conid) {
			this.conid = conid;
			res = p.hgetAll("" + conid);
		}

		public Prices getPrices() {
			return new Prices(res.get() );
		}
	}

	interface JRun {
		public void run(Pipeline p);
	}
	
	
	
	/** Top-level method. */
	private void getAllPrices() throws RefException {
		S.out( "Returning all prices");

		// send a single query to Redis for the prices
		ArrayList<PriceQuery> list = new ArrayList<PriceQuery>(); 
		m_main.jquery( pipeline -> {
			for (Object obj : m_main.stocks() ) {
				StringJson stk = (StringJson)obj;
				list.add( new PriceQuery(pipeline, stk.getInt("conid") ) );
			}
		});

		// build the json response
		JSONObject whole = new JSONObject();
		for (PriceQuery q : list) {
			Prices prices = q.getPrices();

			JSONObject single = new JSONObject();
			single.put( "bid", round( prices.anyBid() ) );
			single.put( "ask", round( prices.anyAsk() ) );
			whole.put( "" + q.conid, single);
		}
		
		respond( new Json( whole) );
	}

	/** this seems useless since you can still be left with .000001 */
	private double round(double val) {
		return Math.round( val * 100) / 100.;
	}

	/** Top-level method. */
	void order(boolean whatIf, boolean fireblocks) throws RefException {
		require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");

		String side = m_map.getRequiredParam( "side");
		require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "Side must be 'buy' or 'sell'");

		double quantity = m_map.getRequiredDouble( "quantity");
		require( quantity > 0.0, RefCode.INVALID_REQUEST, "Quantity must be positive");
		
		double price = m_map.getRequiredDouble( "price");
		require( price > 0, RefCode.INVALID_REQUEST, "Price must be positive");

		double amt = price * quantity;
		double maxAmt = side == "buy" ? Main.m_config.maxBuyAmt() : Main.m_config.maxSellAmt();
		require( amt <= maxAmt, RefCode.ORDER_TOO_LARGE, "The total amount of your order (%s) exceeds the maximum allowed amount of %s", S.formatPrice( amt), S.formatPrice( maxAmt) ); // this is displayed to user

		String wallet = null;
		String cryptoId = null;
		if (!whatIf) {
			wallet = m_map.getRequiredParam("wallet");
			cryptoId = m_map.getRequiredParam("cryptoid");
		}
		
		// calculate order price
		double prePrice;
		if (side == "buy") {
			prePrice = price - price * Main.m_config.minBuySpread();
		}
		else {
			prePrice = price + price * Main.m_config.minSellSpread();
		}
		double orderPrice = Util.round( prePrice);  // round to two decimals
		
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		Order order = new Order();
		order.action( side == "buy" ? Action.BUY : Action.SELL);
		order.totalQuantity( quantity);
		order.lmtPrice( orderPrice);
		order.tif( TimeInForce.IOC);
		order.whatIf( whatIf);
		order.transmit( true);
		order.outsideRth( true);
		order.cryptoId( cryptoId);
		order.walletAddr( wallet);
		
		S.out( "Requesting contract details for %s on %s", conid, contract.exchange() );
		
		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( !list.isEmpty(), RefCode.INVALID_REQUEST, "No contract details");
				
				ContractDetails deets = list.get(0);
				require( inside( deets.conid(), deets.liquidHours(), deets.timeZoneId() ) ||
				         inside( deets.conid(), deets.tradingHours(), deets.timeZoneId() ), RefCode.EXCHANGE_CLOSED, "Exchange is closed");

				// check that we have prices and that they are within bounds; 
				// do this after checking trading hours because that would 
				// explain why there are no prices which should never happen otherwise
				Prices prices = getPrices( contract.conid() );
				prices.checkOrderPrice( order, orderPrice, Main.m_config);
				
				// if the user submitted an order for < .5 shares, we round to zero so no order is placed
				if (whatIf) {
					// for what-if, submit it with at least qty of 1 to make sure it is a valid order 
					if (order.roundedQty() == 0) {
						order.totalQuantity(1);
					}
					
					log( LogType.CHECK, order.getCheckLog(contract) );
					submitWhatIf( contract, order);
				}
				else {
					log( LogType.ORDER, order.getOrderLog(contract) );

					// if order size < .5, we won't submit an order; better would be to compare our total share balance with the total token balance. pas 
					if (order.roundedQty() == 0) {
						respondToOrder(order, 0, false, OrderStatus.Filled, fireblocks);
					}
					else {
						submitOrder(  contract, order, fireblocks);
					}
				}
			});
		});
	}
	
	private void submitWhatIf( Contract contract, final Order order) throws RefException {
		// check trading hours first since it is a nicer error message
		
		// simulated trading?
		if (Main.simulated() ) {
			respond( code, RefCode.OK);
			return;
		}
		
		// submit what-if order
		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderState(OrderState orderState) {
				wrap( () -> {
					S.out( "  rec what-if orderState  id=%s  %s", order.orderId(), Main.tos( orderState) );
					double initMargin = Double.valueOf( orderState.initMarginAfter() );
					double elv = Double.valueOf( orderState.equityWithLoanAfter() );
					require( initMargin <= elv, RefCode.REJECTED, "Insufficient liquidity in brokerage account");
					respond( code, RefCode.OK);
				});
			}
			@Override public void handle(int errorCode, String errorMsg) {
				wrap( () -> {
					S.out( "  rec what-if error for %s; %s - %s", order.orderId(), errorCode, errorMsg);

					// NOTE that this code is duplicated in what-if handler
					
					// price does not conform to market rule, e.g. too many decimals
					if (errorCode == 110) {
						require( false, RefCode.INVALID_PRICE, errorMsg);
					}
					require( false, RefCode.UNKNOWN, errorCode + " - " + errorMsg);
				});
			}
		});

		setTimer( Main.m_config.timeout(), () -> timedOut( "checkorder timed out") );
	}

	private void submitOrder( Contract contract, Order order, boolean fireblocks) throws RefException {
		ModifiableDecimal shares = new ModifiableDecimal();

		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
				
				wrap( () -> {
					S.out( "  order status  id=%s  status=%s", order.orderId(), status);
	
					// save the number of shares filled
					shares.value = filled.toDouble();
					
					// better is: if canceled w/ no shares filled, let it go to handle() below
					
					if (status.isComplete() ) {
						respondToOrder( order, shares.value(), false, status, fireblocks);
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
		
		log( LogType.SUBMIT, "wallet=%s  cryptoid=%s  orderid=%s",
				order.walletAddr(), order.cryptoId(), order.orderId() );
		
		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( Main.m_config.orderTimeout(), () -> onTimeout( order, shares.value(), OrderStatus.Unknown, fireblocks) );
	}
	
	private synchronized void onTimeout(Order order, double filledShares, OrderStatus status, boolean fireblocks) throws Exception {
		// this could happen if our timeout is lower than the timeout of the IOC order,
		// which should never be the case
		if (!m_responded) {
			log( LogType.ORDER_TIMEOUT, "id=%s  cryptoid=%s   order timed out with %s shares filled and status %s", order.orderId(), order.cryptoId(), filledShares, status);

			// if order is still live, cancel the order 
			if (!status.isComplete() && !status.isCanceled() ) {
				S.out( "Canceling order %s on timeout", order.orderId() );
				m_main.orderController().cancelOrder( order.orderId(), "", null);
			}
	
			respondToOrder( order, filledShares, true, status, fireblocks);
		}
	}
	
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized.
	 *  In the case where order qty < .5 and we didn't submit an order,
	 *  orderStatus will be Filled. */ 
	private synchronized void respondToOrder(Order order, double filledShares, boolean timeout, OrderStatus status, boolean fireblocks) throws Exception {
		if (m_responded) {
			return;    // this happens when the timeout occurs after an order is filled, which is normal
		}
		
		// no shares filled and order size >= .5?
		if (filledShares == 0 && status != OrderStatus.Filled) {  // Filled status w/ zero shares means order size was < .5
			String msg = timeout ? "Order timed out, please try again" : "The order could not be filled; it may be that the price changed. Please try again.";
			respond( code, RefCode.REJECTED, text, msg);
			log( LogType.REJECTED, "id=%s  cryptoid=%s  orderQty=%s  orderPrc=%s  reason=%s", 
					order.orderId(), order.cryptoId(), order.totalQty(), order.lmtPrice(), msg);
			return;
		}

		double stockQty;  // quantity of stock tokens to swap
		LogType logType;
		RefCode refCode;
		double tds = 0;
		String hash = "";

		// for a filled order, the (order size - filled size) should always be <= .5
		// if > .5, then it was a partial fill and we will use the filled size instead of the order size
		// this way we always have max .5 shares difference between stock pos and token pos (for a single order)
		if (order.totalQuantity() - filledShares > .5001) {
			stockQty = filledShares;
			logType = LogType.PARTIAL_FILL;
			refCode = RefCode.PARTIAL_FILL;
		}
		else {
			stockQty = order.totalQuantity();
			logType = LogType.FILLED;
			refCode = RefCode.OK;
		}
		
		if (fireblocks) {
			try {
				String id;
				
				if (order.action() == Action.BUY) {
					double stablecoinAmt = stockQty * order.lmtPrice() + Main.m_config.commission();
					id = Rusd.buyStock(order.walletAddr(), order.stablecoinAddr(), stablecoinAmt, 
							order.stockTokenAddr(), stockQty);
				}
				else {
					double preAmt = stockQty * order.lmtPrice() - Main.m_config.commission();
					tds = .01 * preAmt;
					double stablecoinAmt = preAmt - tds;  
					id = Rusd.sellStock(order.walletAddr(), Rusd.rusdAddr, stablecoinAmt,
							order.stockTokenAddr(), stockQty);
				}
				
				// it would be better if we could send back the response in two blocks, one
				// when the order fills and one when the blockchain transaction is completed
				
				// wait for the transaction to be signed
				hash = Deploy.getTransHash(id, 60);  // do we really need to wait this long? pas
				log( LogType.ORDER, "Order %s completed Fireblocks transaction with hash %s", order.orderId(), hash);
			}
			catch( Exception e) {
				e.printStackTrace();
				log( LogType.ERROR, "Fireblocks failed for order %i - %s", order.orderId(), e.getMessage() );
				respond( code, RefCode.BLOCKCHAIN_FAILED, text, "Blockchain transaction failed; please try again");
				unwindOrder(order);
				return;
			}
		}

		respond( code, refCode, "filled", stockQty);

		log( logType, "id=%s  cryptoid=%s  action=%s  orderQty=%s  filled=%s  orderPrc=%s  commission=%s  tds=%s  hash=%s", 
				order.orderId(), order.cryptoId(), order.action(), order.totalQty(), 
				S.fmt3(filledShares), order.lmtPrice(), 
				Main.m_config.commission(), tds, hash);
	}
	
	/** The order was filled, but the blockchain transaction failed, so we must unwind the order. */
	private void unwindOrder(Order order) {
		// send an alert to the operator to manually unwind the order for now. pas
	}

	synchronized boolean respond( Object...data) {
		return respond( Util.toJsonMsg( data) );
	}
	
	/** Only respond once for each request
	 *  @return true if we responded just now. */
	synchronized boolean respond( Json response) {
		if (!m_responded) {
			// need this? pas
			//String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());
	
			try {
				OutputStream outputStream = m_exchange.getResponseBody();
				m_exchange.getResponseHeaders().add( "Content-Type", "application/json");
				
				m_exchange.sendResponseHeaders(200, response.length());
				outputStream.write(response.getBytes());
				outputStream.close();
				//log( "%s %s ~ %s", Util.now(), m_map.getLog(), response.getLog() );
				
			}
			catch (Exception e) {
				e.printStackTrace();
				log( LogType.ERROR, "Exception while responding");
			}
			finally {
				m_responded = true;
				return true;
			}
		}
		return false;
	}

	void wrap( RefRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			boolean responded = respond( e.toJson() );
			
			// display log except for timeouts where we have already responded
			if (responded || e.code() != RefCode.TIMED_OUT) {
				log( LogType.ERROR, e.toString() );
			}
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			respond( code, RefCode.UNKNOWN, text, e.getMessage() );  // could there be invalid characters? pas
		}
	}
	
	interface RefRunnable {
		void run() throws Exception; 
	}

	void setTimer( long ms, RefRunnable runnable) {
		Timer timer = new Timer();
		timer.schedule( new TimerTask() {  // this could be improved to have only one Timer and hence one Thread for all the scheduling. pas
			@Override public void run() {
				wrap( runnable);
				timer.cancel();
			}
		}, ms);
	}
	
	static void timedOut( String text, Object... params) throws RefException {
		throw new RefException( RefCode.TIMED_OUT, text, params);
	}

	static class ModifiableDecimal {
		private double value = 0;

		@Override public String toString() { 
			return S.fmt3(value);
		}
		
		public double value() {
			return this.value;
		}

		boolean isZero() {
			return value == 0;
		}
		
		boolean nonZero() {
			return value != 0;
		}
	};
	
}

// with 2 sec timeout, we see timeout occur before fill is returned
// add tests for partial fill, no fill, and order size < .5
// confirm that TWS does not accept fractional shares
// test w/ a short timeout to see the timeout happen, ideally with 0 shares and partial fill
