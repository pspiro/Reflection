package reflection;

import static reflection.Main.log;
import static reflection.Main.require;
import static reflection.Main.round;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController.IPositionHandler;
import com.sun.net.httpserver.HttpExchange;

import fireblocks.Erc20;
import fireblocks.Fireblocks;
import fireblocks.StockToken;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import redis.clients.jedis.Jedis;
import reflection.SiweTransaction.Session;
import tw.google.Auth;
import tw.google.TwMail;
import tw.util.S;
import util.LogType;

/** This class handles events from the Backend. See subclass */
public class MyTransaction {
	enum Stablecoin {
		RUSD, BUSD
	}
	enum MsgType {
		checkHours,
		disconnect,
		dump,
		getAllPrices,
		getAllStocks,
		getConfig,
		getConnectionStatus,
		getDescription,
		getPositions,
		getPrice,
		mint,
		pullBackendConfig,
		pullFaq,
		pushBackendConfig,
		pushFaq,
		refreshConfig,
		refreshStocks,
		seedPrices,
		terminate,
		testAlert,
		wallet,
		;

		public static String allValues() {
			return Arrays.asList( values() ).toString();
		}
	}


	static double SMALL = .0001; // if difference between order size and fill size is less than this, we consider the order fully filled
	static final String code = "code";
	static final String text = "text";
	public static final String exchangeIsClosed = "The exchange is closed. Please try your order again after the stock exchange opens. For US stocks and ETF's, this is usually 4:00 EST (14:30 IST).";
	public static final String etf = "ETF";  // must match type column from spreadsheet
	private static final String ibeos = "IBEOS";  // IB exchange w/ 24 hour trading for ETF's
	
	protected Main m_main;
	protected HttpExchange m_exchange;
	protected boolean m_responded;  // only respond once per transaction
	protected ParamMap m_map = new ParamMap();
	protected String uri;

	MyTransaction( Main main, HttpExchange exchange) {
		m_main = main;
		m_exchange = exchange;
		uri = Main.getURI(m_exchange);  // this prints it out
	}

	void handle() {
		wrap( () -> {
			parseMsg();
			handleMsg();
		});
	}

	// you could encapsulate all these methods in MyExchange

	/** keys are all lower case */
	void parseMsg() throws Exception {
		require( uri.length() < 4000, RefCode.INVALID_REQUEST, "URI is too long");

		if ("GET".equals(m_exchange.getRequestMethod() ) ) {
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

		else {
			try {
	            Reader reader = new InputStreamReader( m_exchange.getRequestBody() );

	            JSONObject jsonObject = (JSONObject)new JSONParser().parse(reader);  // if this returns a String, it means the text has been over-stringified (stringify called twice)

	            for (Object key : jsonObject.keySet() ) {
	            	Object value = jsonObject.get(key);
	            	require( key instanceof String, RefCode.INVALID_REQUEST, "Invalid JSON, key is not a string");

	            	if (value != null) {
	            		m_map.put( (String)key, value.toString() );
	            	}
	            }

	            S.out( "  received POST request " + jsonObject);
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
	}

	void handleMsg() throws Exception {

		MsgType msgType = m_map.getEnumParam( "msg", MsgType.values() );

		switch (msgType) { // this could be switched to polymorphism if desired
			case mint:
				mint();
				break;
			case getPrice:
				getPrice();
				break;
			case getAllPrices:
				getAllPrices();
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
//			case pullBackendConfig:
//				pullBackendConfig();
//				break;
//			case pullFaq:
//				pullFaq();
//				break;
//			case pushFaq:
//				pushFaq();
//				break;
			case terminate:
				terminate();
				break;
			case disconnect:
				disconnect();
				break;
			case dump:
				m_main.dump();
				respondOk();
				break;
			case getPositions:
				getStockPositions();
				break;
			case testAlert:
				onTest();
				break;
			case seedPrices:
				onSeedPrices();
				break;
			case wallet:
				onShowWallet();
				break;
		}
	}

	private void onShowWallet() throws Exception {
		String wallet = m_map.getRequiredParam("address");

		JSONObject obj = new JSONObject();
		obj.put( "Native token balance", MoralisServer.getNativeBalance(wallet) );
		obj.put( "Allowance", Main.m_config.busd().getAllowance(wallet, Main.m_config.rusdAddr() ) );
		obj.put( "Positions", MoralisServer.reqPositions(wallet).getArray() );
		respond(obj);
	}

	private void onTest() {
		S.out( "Sending test alert");
		alert("TEST", "This is a test of the alert system");
		respondOk();
	}

	/** Used by the Monitor program */
	private void getStockPositions() {
		JSONArray ar = new JSONArray();
		
		m_main.orderController().reqPositions( new IPositionHandler() {
			@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
				JSONObject obj = new JSONObject();
				obj.put( "conid", contract.conid() );
				obj.put( "position", pos.toDouble() );
				ar.add( obj);
			}
			@Override public void positionEnd() {
				respond(ar); 
			}
		});
		
		setTimer( Main.m_config.timeout(), () -> timedOut( "getPositions timed out") );
	}

	/** Top-level message handler. This version takes wallet param; you can also call
	 *  reflection.trading/mint/0xxxx.xxx */
	void mint() throws Exception {
		Main.mint( m_map.getRequiredParam( "wallet") );
		respond( code, "OK");
	}

	/** Simulate disconnect to test reconnect */
	private void disconnect() {
		S.out( "simulating disconnecting");
		m_main.orderConnMgr().disconnect();
		respondOk();
	}

	private void terminate() {
		log( LogType.TERMINATE, "");
		System.exit( 0);
	}

	/** Top-level message handler */
	private void pushBackendConfig() throws Exception {
		S.out( "Pushing backend config from google sheet to database");
		Main.m_config.pushBackendConfig( m_main.sqlConnection() );
		respondOk();
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
		S.out( "Refreshing config and FAQs from google sheet");
		m_main.readConfigurations();
		respondOk();
	}

	/** Top-level message handler */
	void refreshStocks() throws Exception {
		S.out( "Refreshing stock list from google sheet");
		m_main.refreshStockList();
		respondOk();
	}

	/** Top-level message handler */
	private void getAllStocks() throws RefException {
		require( !m_main.stocks().isEmpty(), RefCode.NO_STOCKS, "We don't have the list of stocks");

		S.out( "Returning all stocks");
		respond(m_main.stocks());
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
				require( list.size() > 0, RefCode.NO_SUCH_STOCK, "No such stock");

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

				respond(whole);
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

			if (inside( deets, deets.liquidHours() ) ) {
				respond( "hours", "liquid");
			}
			else if (inside( deets, deets.tradingHours() ) ) {
				respond( "hours", "illiquid");
			}
			else {
				respond( "hours", "closed");
			}
		});
	}

	/** Top-level method - old style */
	private void getPrice() throws RefException {
		int conid = m_map.getRequiredInt( "conid");
		returnPrice(conid);
	}

	/** @return e.g. { "bid": 128.5, "ask": 128.78 } */
	void returnPrice(int conid) throws RefException {
		Prices prices = m_main.getPrices( conid);
		require(prices.hasAnyPrice(), RefCode.NO_PRICES, "No prices available for conid %s", conid);

		S.out( "Returning prices  bid=%s  ask=%s  for conid %s", prices.bid(), prices.ask(), conid);
		respond( prices.toJson(conid) );
	}

	/** Top-level method. */  // put a timer so we don't call this more than once every n ms. pas
	private void getAllPrices() throws RefException {
		S.out( "Returning all prices");

		boolean admin = m_map.getBool("admin");

		// build the json response   // we could reuse this and just update the prices each time
		JSONObject whole = new JSONObject();
		for (Object obj : m_main.stocks() ) {
			Stock stk = (Stock)obj;

			JSONObject single = new JSONObject();
			// in admin mode, which is for debugging, return the actual prices
			if (admin) {
				single.put( "bid", round( stk.prices().bid() ) );
				single.put( "ask", round( stk.prices().ask() ) );
				single.put( "last", round( stk.prices().last() ) );
				single.put( "close", round( stk.prices().close() ) );
				single.put( "time", stk.prices().getFormattedTime() );
			}
			// for the user, we return best bid/ask we can come up with
			else {                                // this part should go away
				single.put( "bid", round( stk.prices().anyBid() ) );
				single.put( "ask", round( stk.prices().anyAsk() ) );
			}
			whole.put( stk.get("conid"), single);
		}

		respond(whole);
	}

	String action() throws RefException { 
		return m_map.getRequiredParam("action"); 
	}
	
	/** Top-level method. */
	protected Erc20 stablecoin() throws Exception {
		return m_map.getEnumParam("currency", Stablecoin.values() ) == Stablecoin.BUSD
				? Main.m_config.busd() : Main.m_config.rusd();
	}
	
	interface Inside {
		void run(boolean inside) throws Exception;
	}
	
	/** Check if we are inside trading hours. For ETF's, check smart; if that fails,
	 *  check IBEOS and change the exchange on the contract passed in to IBEOS.
	 *  Note that the callback is wrapped */
	void insideAnyHours( Contract contract, Inside runnable) {
		insideHours( contract, inside -> {
			
			// for testing
			if (Main.m_config.autoFill() ) {
				runnable.run(true);
				return;
			}
			
			if (!inside && etf.equals( m_main.getType( contract.conid() ) ) ) {
				contract.exchange(ibeos);
				insideHours( contract, runnable);
			}
			else {
				runnable.run(inside);
			}
		});
	}

	/** Call true or false on the Inside runnable */
	void insideHours( Contract contract, Inside runnable) {
		m_main.orderController().reqContractDetails(contract, list -> {
			wrap( () -> {
				require( !list.isEmpty(), RefCode.INVALID_REQUEST, "No contract details");

				ContractDetails deets = list.get(0);
				deets.simTime( m_map.getParam("simtime") );  // this is for use by the test scripts in TestOutsideHours only

				runnable.run( inside( deets) );
			});
		});
	}

	/** Return true if we are current inside trading hours OR liquid hours.
	 *  When running test scripts, simTime param will be set and used. */
	static boolean inside( ContractDetails deets) throws Exception {
		return inside( deets, deets.tradingHours() ) ||
			   inside( deets, deets.liquidHours() );
	}

	/** Return true if we are inside the specified hours; uses deets.simTime if set.
	 * @throws Exception */
	static boolean inside(ContractDetails deets, String hours) throws Exception {
		return Util.inside( deets.getNow(), deets.conid(), hours, deets.timeZoneId() );
	}

	private void submitWhatIf( Contract contract, final Order order) throws Exception {
		// check trading hours first since it is a nicer error message

		// submit what-if order
		m_main.orderController().placeOrModifyOrder(contract, order, new OrderHandlerAdapter() {
			@Override public void orderState(OrderState orderState) {
				wrap( () -> {
					S.out( "  rec what-if orderState  id=%s  %s", order.orderId(), Main.tos( orderState) );
					double initMargin = Double.valueOf( orderState.initMarginAfter() );
					double elv = Double.valueOf( orderState.equityWithLoanAfter() );
					if (initMargin > elv) {
						alert( "WARNING: LOW LIQUIDITY IN BROKERAGE ACCOUNT", "");
						throw new RefException(RefCode.REJECTED, "Insufficient liquidity in brokerage account");
					}
					respondOk();
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

	public void respondOk() {
		respond( code, RefCode.OK);
	}

	/** @param data is an array of key/value pairs */
	synchronized boolean respond( Object...data) {     // this is dangerous and error-prone because it could conflict with the version below
		if (data.length > 1 && data.length % 2 == 0) {
			return respondFull( Util.toJsonMsg( data), 200, null);
		}
		
		// can't throw an exeption here
		Exception e = new Exception("respond(Object...) called with wrong number of params");
		e.printStackTrace();
		return respondFull( RefException.eToJson(e, RefCode.UNKNOWN), 400, null);
	}

	/** Only respond once for each request
	 *  @return true if we responded just now. */
	boolean respond( JSONAware response) {
		return respondFull( response, 200, null);
	}

	/** @param responseCode is 200 or 400 */
	synchronized boolean respondFull( JSONAware response, int responseCode, HashMap<String,String> headers) {
		if (m_responded) {
			return false;
		}
		
		// need this? pas
		try (OutputStream outputStream = m_exchange.getResponseBody() ) {
			m_exchange.getResponseHeaders().add( "Content-Type", "application/json");

			// add custom headers, if any  (add URL encoding here?)
			if (headers != null) {
				for (Entry<String, String> header : headers.entrySet() ) {
					m_exchange.getResponseHeaders().add( header.getKey(), header.getValue() );
				}
			}
			
			String data = response.toString();
			m_exchange.sendResponseHeaders( responseCode, data.length() );
			outputStream.write(data.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while responding with json");
		}
		m_responded = true;
		return true;
	}

	/** The main difference between Exception and RefException is that Exception is not expected and will print a stack trace.
	 *  Also Exception returns code UNKNOWN since none is passed with the exception */
	void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( RefException e) {
			boolean responded = respondFull( 
					e.toJson(), 
					400, 
					null);  // return false if we already responded

			// display log except for timeouts where we have already responded
			if (responded || e.code() != RefCode.TIMED_OUT) {
				log( LogType.ERROR, e.toString() );
			}
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, S.notNull( e.getMessage() ) );
			respondFull( 
					RefException.eToJson(e, RefCode.UNKNOWN),
					400,
					null);
		}
	}

	/** Runnable, returns void, throws Exception */
	public interface ExRunnable {
		void run() throws Exception;
	}

	void setTimer( long ms, ExRunnable runnable) {
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

	/** don't throw an exception here, it should not disrupt any other process */
	protected void alert(String subject, String body) {
		try {
			TwMail mail = Auth.auth().getMail();
			mail.send(
					"RefAPI", 
					"peteraspiro@gmail.com", 
					"peteraspiro@gmail.com",
					subject,
					body,
					"plain");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	
	/** Validate the cookie or throw exception, and update the access time on the cookie 
	 * @param userAddr */
	void validateCookie(String walletAddr) throws Exception {
		// we can take cookie from map or header
		// cookie format is <cookiename=cookievalue> where cookiename is <__Host_authToken><wallet_addr><chainid>
		String cookie = m_map.get("cookie");
		if (cookie == null) {
			cookie = SiweTransaction.findCookie( m_exchange.getRequestHeaders(), "__Host_authToken");
		}
		Main.require(cookie != null, RefCode.INVALID_REQUEST, "Null cookie");
		
		// un-do the URL encoding
		cookie = URLDecoder.decode(cookie);
		Main.require(cookie.split("=").length >= 2, RefCode.INVALID_REQUEST, "Malformed cookie, no '=': " + cookie);
		
		// parse cookie (in header, it has two fields, signature and message; in map, it has only message)
		MyJsonObject siweMsg = MyJsonObject.parse( cookie.split("=")[1])
				.getObj("message");
		Main.require( siweMsg != null, RefCode.INVALID_REQUEST, "Malformed cookie: " + cookie);
		
		// find session object
		Session session = SiweTransaction.sessionMap.get( siweMsg.getString("address") );
		Main.require(session != null, RefCode.INVALID_REQUEST, "No session object found for address " + siweMsg.getString("address") );

		// valiate nonce
		Main.require(
				session.nonce().equals( siweMsg.getString("nonce") ),
				RefCode.VALIDATION_FAILED,
				"Nonce does not match");

		// check for expiration
		Main.require( 
				System.currentTimeMillis() - session.lastTime() <= Main.m_config.sessionTimeout(),
				RefCode.SESSION_EXPIRED,
				"Session has expired");
		
		// confirm no wallet or same wallet
		Main.require( 
				walletAddr == null || walletAddr.equalsIgnoreCase(siweMsg.getString("address") ), 
				RefCode.VALIDATION_FAILED, 
				"The address on the message does not match the address of the session");
		
		// update expiration time
		session.update();
	}
	
	/** top-level method; set some prices for use in test systems */
	void onSeedPrices() {
		wrap( () -> {
			Jedis jedis = Main.m_config.redisPort() == 0
				? new Jedis( Main.m_config.redisHost() )  // use full connection string
				: new Jedis( Main.m_config.redisHost(), Main.m_config.redisPort() );
	
			jedis.hset( "8314", "bid", "128.20");
			jedis.hset( "8314", "ask", "128.30");
			jedis.hset( "13824", "bid", "148.48");
			jedis.hset( "13824", "ask", "148.58");
			jedis.hset( "13977", "bid", "116.05");
			jedis.hset( "13977", "ask", "116.15");
			jedis.hset( "265598", "bid", "165.03");
			jedis.hset( "265598", "ask", "165.13");
			jedis.hset( "320227571", "bid", "318.57");
			jedis.hset( "320227571", "ask", "328.57");
			
			m_map.put("admin", "true");
			getAllPrices();
		});
	}
}

// with 2 sec timeout, we see timeout occur before fill is returned
// add tests for partial fill, no fill
// confirm that TWS does not accept fractional shares
// test w/ a short timeout to see the timeout happen, ideally with 0 shares and partial fill
// test exception during fireblocks part
// IOC timeout seems to be 3-4 seconds
// don't store commission in two places in db