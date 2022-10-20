package reflection;

import static reflection.Main.log;
import static reflection.Main.require;
import static reflection.Util.inside;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
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

import reflection.Main.Mode;
import tw.util.S;
import util.LogType;

class MyTransaction {
	enum MsgType {
		getPrice, order, checkOrder, checkHours, getAllPrices, getDescription, getAllStocks, refreshStockList, getConfig, refreshConfig, pushBackendConfig, pullBackendConfig, getConnectionStatus, terminate;

		public static String allValues() {
			return Arrays.asList( values() ).toString();
		}
	}
	

	static double SMALL = .001; // if difference between order size and fill size is less than this, we consider the order fully filled
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
				order( false);
				break;
			case checkOrder:
				order( true);
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
			case refreshStockList:
				refreshStockList();
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
			case terminate:
				terminate();
				break;
		}
	}
	
	private void terminate() {
		log( LogType.TERMINATE, "");
		System.exit( 0);
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
		respond( "connectedToTWS", m_main.m_controller.isConnected(), "connectedToBroker", m_main.m_ibConnection);
	}

	/** Top-level message handler */ 
	void getConfig() throws Exception {
		S.out( "Sending config");
		respond( Main.m_config.toJson() );
	}
	
	/** Top-level message handler */ 
	void refreshConfig() throws Exception {
		S.out( "Refreshing config from google sheet");
		Main.m_config.readFromSpreadsheet("Config");
		respond( Main.m_config.toJson() );
	}
	
	/** Top-level message handler */ 
	void refreshStockList() throws Exception {
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
		require( m_main.m_controller.isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		S.out( "Returning stock description");
		Contract contract = new Contract();
		contract.secType( SecType.STK);
		contract.symbol( m_map.getRequiredParam("symbol").replace( '_', ' ') );
		contract.exchange( m_map.getParam("exchange") );
		contract.currency( m_map.getParam("currency") );

		m_main.m_controller.reqContractDetails(contract, list -> {
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
		require( m_main.m_controller.isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		
		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "Param 'conid' must be positive integer");

		S.out( "Returning trading hours for %s", conid);
		Contract contract = new Contract();
		contract.conid( conid);
		contract.exchange( m_main.getExchange( conid) );

		m_main.m_controller.reqContractDetails(contract, list -> processHours( conid, list) );
		
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
	private void getPrice() throws RefException {
		require( m_main.m_controller.isConnected(), RefCode.NOT_CONNECTED, "Not connected");

		int conid = m_map.getRequiredInt( "conid");

		Prices prices = m_main.m_priceMap.get( conid);
		require(prices != null && prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		S.out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.bid(), prices.ask(), conid);
		respond( prices.toJson(conid) );
	}
	
	/** Top-level method. */
	private void getAllPrices() throws RefException {
		require( m_main.m_controller.isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( !m_main.m_priceMap.keySet().isEmpty(), RefCode.NO_PRICES, "There are no prices available.");
		
		S.out( "Returning all prices");
		
		JSONObject whole = new JSONObject();
		
		for (Integer conid : m_main.m_priceMap.keySet() ) {
			Prices prices = m_main.m_priceMap.get(conid);
			if (prices.hasAnyPrice() ) {
				if (Main.simulated() ) {
					prices.adjustPrices();
				}

				JSONObject single = new JSONObject();
				single.put( "bid", round( prices.anyBid() ) );
				single.put( "ask", round( prices.anyAsk() ) );

				whole.put( String.valueOf( conid), single); 
			}
		}

		respond( new Json( whole) );
	}

	private double round(double val) {
		return Math.round( val * 100) / 100.;
	}

	/** Top-level method. */
	void order(boolean whatIf) throws RefException {
		require( m_main.m_controller.isConnected(), RefCode.NOT_CONNECTED, "Not connected");
		require( m_main.m_ibConnection, RefCode.NOT_CONNECTED, "No connection to broker");

		int conid = m_map.getRequiredInt( "conid");
		require( conid > 0, RefCode.INVALID_REQUEST, "'conid' must be positive integer");

		String side = m_map.getRequiredParam( "side");
		require( side == "buy" || side == "sell", RefCode.INVALID_REQUEST, "side must be 'buy' or 'sell'");

		double dblQty = m_map.getRequiredDouble( "quantity");
		require( dblQty > 0.0, RefCode.INVALID_REQUEST, "quantity must be positive");
		int quantity = (int)Math.round( dblQty);
		
		double price = m_map.getRequiredDouble( "price");
		require( price > 0, RefCode.INVALID_REQUEST, "price must be positive");

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
		order.totalQuantity( Decimal.get( quantity) );
		order.lmtPrice( orderPrice);
		order.tif( TimeInForce.IOC);
		order.whatIf( whatIf);
		order.transmit( Main.m_config.mode() == Mode.paper);  // don't transmit real orders for now
		order.outsideRth( true);
		order.cryptoId( cryptoId);
		order.wallet( wallet);
		
		S.out( "Requesting contract details for %s", conid);
		
		m_main.m_controller.reqContractDetails(contract, list -> {
			wrap( () -> {
				require( !list.isEmpty(), RefCode.INVALID_REQUEST, "No contract details");
				
				ContractDetails deets = list.get(0);
				require( inside( deets.conid(), deets.liquidHours(), deets.timeZoneId() ) ||
				         inside( deets.conid(), deets.tradingHours(), deets.timeZoneId() ), RefCode.EXCHANGE_CLOSED, "Exchange is closed");

				// check that we have prices and that they are within bounds; 
				// do this after checking trading hours because that would 
				// explain why there are no prices which should never happen otherwise
				Prices prices = m_main.m_priceMap.get( contract.conid() );
				require( prices != null, RefCode.REJECTED, "Prices are not available");
				prices.checkOrderPrice( order, orderPrice, Main.m_config);
				
				// if the user submitted a fractional quantity and it got rounded down to zero, approve the transaction
				if (quantity == 0) {
					respond( code, RefCode.OK);
				}
				else if (whatIf) {
					log( LogType.CHECK, order.getCheckLog(contract) );
					submitWhatIf(  contract, order);
				}
				else {
					log( LogType.ORDER, order.getOrderLog(contract) );
					submitOrder(  contract, order);
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
		m_main.m_controller.placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
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

	private void submitOrder( Contract contract, Order order) throws RefException {
		ModifiableDecimal shares = new ModifiableDecimal();

		// simulated trading?
		if (Main.simulated() ) {
			shares.value = order.totalQuantity(); 
			respondToOrder( order, shares, false, OrderStatus.Unknown);
			return;
		}
		
		m_main.m_controller.placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
				
				wrap( () -> {
					S.out( "  order status  id=%s  status=%s", order.orderId(), status);
	
					// save the number of shares filled
					shares.value = filled;
					
					// better is: if canceled w/ no shares filled, let it go to handle() below
					
					if (status.isComplete() ) {
						respondToOrder( order, shares, false, status);
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
				order.wallet(), order.cryptoId(), order.orderId() );
		
		// use a higher timeout here; it should never happen since we use IOC
		// order timeout is a special case because there could have been a partial fill
		setTimer( Main.m_config.orderTimeout(), () -> respondToOrder( order, shares, true, OrderStatus.Unknown) );
	}
	
	/** This is called when order status is "complete" or when timeout occurs.
	 *  Access to m_responded is synchronized. */ 
	private synchronized void respondToOrder(Order order, ModifiableDecimal shares, boolean timeout, OrderStatus status) {
		if (!m_responded) {
			if (timeout) {
				log( LogType.ORDER_TIMEOUT, "id=%s  cryptoid=%s   order timed out with %s shares filled and status %s", order.orderId(), order.cryptoId(), shares, status);
				
				if (!status.isComplete() && !status.isCanceled() ) {
					S.out( "Canceling order %s", order.orderId() );
					m_main.m_controller.cancelOrder( order.orderId(), "", null);
				}
			}

			
			if (shares.isZero() ) {
				String msg = timeout ? "Order timed out" : "Reason unknown";
				respond( code, RefCode.REJECTED, text, msg);
				log( LogType.REJECTED, "id=%s  cryptoid=%s  orderQty=%s  orderPrc=%s  reason=%s", 
						order.orderId(), order.cryptoId(), order.totalQuantity(), order.lmtPrice(), msg);
			}
			else {
				LogType logType;

				if (status == OrderStatus.Filled || Util.difference( shares.value, order.totalQuantity() ) < SMALL) {
					logType = LogType.FILLED;
					respond( code, RefCode.OK, "filled", shares);
				}
				else {
					logType = LogType.PARTIAL_FILL;
					respond( code, RefCode.PARTIAL_FILL, "filled", shares);
				}
				
				log( logType, "id=%s  cryptoid=%s  orderQty=%s  filled=%s  orderPrc=%s", order.orderId(), order.cryptoId(), order.totalQuantity(), shares, order.lmtPrice() );
			}
		}
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
		Decimal value = Decimal.ZERO;

		@Override public String toString() { return value.toString(); }
		
		boolean isZero() {
			return !nonZero();
		}
		
		boolean nonZero() {
			return Decimal.isValidNotZeroValue(value);
		}
	};
	
}

// with 2 sec timeout, we see timeout occur before fill is returned
