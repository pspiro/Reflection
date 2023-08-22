package reflection;

import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.OrderState;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import common.Util;
import http.MyHttpClient;
import redis.MyRedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;
import reflection.Config.RefApiConfig;
import reflection.MySqlConnection.SqlRunnable;
import test.MyTimer;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

public class Main implements ITradeReportHandler {
	enum Status {
		Connected, Disconnected
	};
	
	private static final Random rnd = new Random( System.currentTimeMillis() );
	static final Config m_config = new RefApiConfig();
	private static final DateLogFile m_log = new DateLogFile("reflection"); // log file for requests and responses
	static GTable m_failCodes;  // table of error codes that we want to fail; used for testing, only read of Config.produceErrors is true

	private       MyRedis m_redis;  // used for periodically querying the prices  // can't be final because an exception can occur before it is initialized 
	private final ConnectionMgr m_orderConnMgr; // we assume that TWS is connected to IB at first but that could be wrong; is there some way to find out?
	private final String m_tabName;
	private       String m_faqs;
	private String m_type1Config; 
	private JsonObject m_type2Config;
	final TradingHours m_tradingHours; 
	private final Stocks m_stocks = new Stocks();
	private GTable m_blacklist;  // wallet is key, case insensitive
	
	JsonArray stocks() { return m_stocks.stocks(); }

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				throw new Exception( "You must specify a config tab name");
			}

			new Main( args[0] );
		}
		catch( BindException e) {
			S.out( "The application is already running");
			e.printStackTrace();
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}

	public Main(String tabName) throws Exception {
		m_tabName = tabName;

		// create log file folder and open log file
		log( LogType.RESTART, Util.readResource( Main.class, "version.txt") );  // print build date/time
		
		MyTimer timer = new MyTimer();

		// read config settings from google sheet; this must go first since other items depend on it
		timer.next("Reading from google spreadsheet %s", tabName, NewSheet.Reflection);
		readSpreadsheet();
		timer.done();
		
		// APPROVE-ALL SETTING IS DANGEROUS and not normal
		// make user approve it during startup
		if (m_config.autoFill() ) {
			S.out( "WARNING: The RefAPI will approve all orders and WILL NOT SEND ORDERS TO THE EXCHANGE");
		}
		
		// check database connection to make sure it's there
		timer.next( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		sqlConnection( conn -> {} );

		// if port is zero, host contains connection string, otherwise host and port are used
		timer.next( "Connecting to redis with %s:%s", m_config.redisHost(), m_config.redisPort() );
		m_redis = new MyRedis(m_config.redisHost(), m_config.redisPort() );
		m_redis.connect(); // this is not required but we want to bail out if redis is not running
		m_redis.setName("RefAPI");

		timer.next( "Starting stock price query thread every n ms");
		Util.executeEvery( 0, m_config.redisQueryInterval(), () -> queryAllPrices() );  // improve this, set up redis stream
		
		// check that Fireblocks server is running
		checkFbActiveServer();
		
		
		
		// /api/crypto-transactions  all trades, I think not used
		// /api/crypto-transactions?wallet_public_key=${address}&sortBy=id:desc  all trades for one user


		timer.next( "Listening on %s:%s  (%s threads)", m_config.refApiHost(), m_config.refApiPort(), m_config.threads() );
		HttpServer server = HttpServer.create(new InetSocketAddress(m_config.refApiHost(), m_config.refApiPort() ), 0);
		//HttpServer server = HttpServer.create(new InetSocketAddress( m_config.refApiPort() ), 0);
		server.createContext("/siwe/signout", exch -> new SiweTransaction( this, exch).handleSiweSignout() );
		server.createContext("/siwe/signin", exch -> new SiweTransaction( this, exch).handleSiweSignin() );
		server.createContext("/siwe/me", exch -> new SiweTransaction( this, exch).handleSiweMe() );
		server.createContext("/siwe/init", exch -> new SiweTransaction( this, exch).handleSiweInit() );
		server.createContext("/mint", exch -> new BackendTransaction(this, exch).handleMint() );
		server.createContext("/favicon", exch -> quickResponse(exch, "", 200) ); // respond w/ empty response
		server.createContext("/api/working-orders", exch -> new LiveOrderTransaction(this, exch).handleLiveOrders() );
		server.createContext("/api/live-orders", exch -> new LiveOrderTransaction(this, exch).handleLiveOrders() );
		server.createContext("/api/users/wallet-update", exch -> new BackendTransaction(this, exch).handleWalletUpdate() );
		server.createContext("/api/users/wallet", exch -> new BackendTransaction(this, exch).handleGetUserByWallet() );
		server.createContext("/api/system-configurations/last", exch -> quickResponse(exch, m_type1Config, 200) );// we can do a quick response because we already have the json
		server.createContext("/api/system-configurations", exch -> quickResponse(exch, "Query not supported", 400) );
		server.createContext("/api/redeemRUSD", exch -> new BackendTransaction(this, exch).handleRedeem() );
		server.createContext("/api/redemptions/redeem", exch -> new BackendTransaction(this, exch).handleRedeem() );
		server.createContext("/api/positions", exch -> new BackendTransaction(this, exch).handleReqPositions() );
		server.createContext("/api/order", exch -> new OrderTransaction(this, exch).backendOrder() );
		server.createContext("/api/ok", exch -> new BackendTransaction(this, exch).respondOk() );
		server.createContext("/api/mywallet", exch -> new BackendTransaction(this, exch).handleMyWallet() );
		server.createContext("/api/hot-stocks", exch -> new BackendTransaction(this, exch).handleHotStocks() );
		server.createContext("/api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/get-stock-with-price", exch -> new BackendTransaction(this, exch).handleGetStockWithPrice() );
		server.createContext("/api/get-price", exch -> new BackendTransaction(this, exch).handleGetPrice() );
		server.createContext("/api/get-profile", exch -> new BackendTransaction(this, exch).handleGetProfile() );
		server.createContext("/api/update-profile", exch -> new BackendTransaction(this, exch).handleUpdateProfile() );
		server.createContext("/api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/fireblocks", exch -> new LiveOrderTransaction(this, exch).handleFireblocks() ); // report build date/time
		server.createContext("/api/faqs", exch -> quickResponse(exch, m_faqs, 200) );
		server.createContext("/api/crypto-transactions", exch -> new BackendTransaction(this, exch).handleReqCryptoTransactions(exch) );
		server.createContext("/api/configurations", exch -> new BackendTransaction(this, exch).handleGetType2Config() );
		server.createContext("/api/about", exch -> new BackendTransaction(this, exch).about() ); // report build date/time
		server.createContext("/", exch -> new OldStyleTransaction(this, exch).handle() );
		server.setExecutor( Executors.newFixedThreadPool(m_config.threads()) );  // multiple threads but we are synchronized for single execution
		server.start();

		m_orderConnMgr = new ConnectionMgr( m_config.twsOrderHost(), m_config.twsOrderPort() );
		m_tradingHours = new TradingHours(orderController()); // must come after ConnectionMgr 

		// connect to TWS
		timer.next( "Connecting to TWS on %s:%s", m_config.twsOrderHost(), m_config.twsOrderPort() );
		m_orderConnMgr.connectNow();  // ideally we would set a timer to make sure we get the nextId message
		timer.done();
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> log(LogType.TERMINATE, "Received shutdown msg from linux kill command")));
	}

	private void checkFbActiveServer() throws Exception {
		try {
			MyHttpClient client = new MyHttpClient("localhost", m_config.fbServerPort() );
			client.get();
			Util.require( client.getResponseCode() == 200, "Error code returned from fireblocks server " + client.getResponseCode() );
		}
		catch( Exception e) {
			throw new Exception("Could not connect to fireblocks server - " + e.getMessage() );
		}
	}

	void readSpreadsheet() throws Exception {
		Book book = NewSheet.getBook(NewSheet.Reflection);
		
		// read RefAPI config
		m_config.readFromSpreadsheet( book, m_tabName );  // must go first

		// read Backend config (used by Frontend)
		readFaqsFromSheet(book);
		m_type1Config = readConfig(book, 1).toString();
		m_type2Config = readConfig(book, 2);

		// read list of RefCodes where we want to simulate failure
		m_failCodes = S.isNotNull( m_config.errorCodesTab() )
			? new GTable( book.getTab(m_config.errorCodesTab()), "Code", "Fail", true)
			: null;
		
		m_blacklist = new GTable( book.getTab("Blacklist"), "Wallet Address", "Allow", false);
		
		m_stocks.readFromSheet(book, m_config);
	}

	/** You could shave 300 ms by sharing the same Book as Config 
	 * @param book */ 
	void readFaqsFromSheet(Book book) throws Exception {
		JsonArray ar = new JsonArray();
		for (ListEntry row : book.getTab( "FAQ").fetchRows() ) {
			if (row.getBool("Active") ) {
				JsonObject obj = new JsonObject();
				obj.put( "question", row.getString("Question") );
				obj.put( "answer", row.getString("Answer") );
				ar.add(obj);
			}
		}
		require( ar.size() > 0, RefCode.CONFIG_ERROR, "You must have at least one active FAQ");
		m_faqs = ar.toString();
	}

	/** You could shave 300 ms by sharing the same Book as Config 
	 * @param book */ 
	JsonObject readConfig(Book book, int type) throws Exception {
		JsonObject obj = new JsonObject();
		for (ListEntry row : book.getTab( m_config.backendConfigTab() ).fetchRows() ) {
			if (row.getInt("Type") == type) {
				obj.put( 
						row.getString("Tag"),  // type-1 entries are all double 
						type == 1 ? row.getDouble("Value") : row.getString("Value") );  // type-2 are all string
			}
		}
		require( obj.size() > 0, RefCode.CONFIG_ERROR, "Type-%s config settings are missing", type);
		return obj;
	}
	
	
	
	// let it fall back to read from a flatfile if this fails. pas

	String getExchange( int conid) throws RefException {
		return getStock(conid).getString("exchange");
	}

	Stock getStock( int conid) throws RefException {
		Stock stock = m_stocks.stockMap().get( conid);
		require(stock != null, RefCode.NO_SUCH_STOCK, "Unknown conid %s", conid);
		return stock;
	}

	// VERY BAD AND INEFFICIENT; build a map. pas; at least change to return Stock
	public HashMap getStockByTokAddr(String addr) throws RefException {
		require(Util.isValidAddress(addr), RefCode.INVALID_REQUEST, "Invalid address %s when getting stock by tok addr", addr);
		
		for (Object obj : m_stocks.stocks() ) {
			HashMap stock = (HashMap)obj;
			if ( ((String)stock.get("smartcontractid")).equalsIgnoreCase(addr) ) {
				return stock;
			}
		}
		return null;
	}


	/** Manage the connection from this client to TWS. */
	class ConnectionMgr implements IConnectionHandler {
		private String m_host;
		private int m_port;
		private int m_clientId;
		private Timer m_timer;
		private boolean m_ibConnection;
		private final ApiController m_controller = new ApiController( this, null, null);
		boolean ibConnection() { return m_ibConnection; }

		ConnectionMgr(String host, int port) {
			m_host = host;
			m_port = port;
			m_clientId = rnd.nextInt( Integer.MAX_VALUE) + 1; // use random client id, but not zero
			
			m_controller.handleExecutions( Main.this);
		}

		public ApiController controller() {
			return m_controller;
		}

		synchronized void startTimer() {
			if (m_timer == null) {
				m_timer = new Timer();
				S.out( "creating timer " + m_timer.hashCode() + " (only one)" );
				m_timer.schedule(new TimerTask() {
					@Override public void run() {
						onTimer();
					}
				}, 0, m_config.reconnectInterval() );
			}
		}

		synchronized void stopTimer() {
			if (m_timer != null) {
				m_timer.cancel();
				m_timer = null;
			}
		}

		void onTimer() {
			try {
				connectNow();
				log( LogType.INFO, "connect() success");
			}
			catch( Exception e) {
				log( LogType.ERROR, "connect() failure");
				m_log.log(e);
			}
		}
		
		synchronized void connectNow() throws Exception {
			log( LogType.INFO, "Connecting to TWS on %s:%s with client id %s", m_host, m_port, m_clientId);
			if (!m_controller.connect(m_host, m_port, m_clientId, "") ) {
				throw new Exception("Could not connect to TWS");
			}
		}

		/** We are connected and have received server version */
		public boolean isConnected() {
			return m_controller.isConnected();
		}

		/** Called when we receive server version. We don't always receive nextValidId. */
		@Override public void onConnected() {
			log( LogType.INFO, "Connected to TWS");
			m_ibConnection = true; // we have to assume it's connected since we don't know for sure

			stopTimer();
			
			m_tradingHours.startQuery();
		}

		/** Ready to start sending messages. */  // anyone that uses requestid must check for this
		@Override public synchronized void onRecNextValidId(int id) {
			// we really don't care if we get this because we are using random
			// order id's; it's because sometimes, after a reconnect or if TWS
			// is just startup up, or if we tried and failed, we don't ever receive
			// it
			log( LogType.INFO, "Received next valid id %s ***", id);  // why don't we receive this after disconnect/reconnect? pas
		}

		@Override public synchronized void onDisconnected() {
			if (m_timer == null) {
				log( LogType.ERROR, "Disconnected from TWS");
				startTimer();
			}
		}

		@Override public void accountList(List<String> list) {
		}

		@Override public void error(Exception e) {
			e.printStackTrace();
		}

		@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
			switch (errorCode) {
				case 1100:
					m_ibConnection = false;
					break;
				case 1102:
					m_ibConnection = true;
					break;
				case 10197:
					S.out( "You can't get market data in your paper account while logged into your production account");
					break;
			}
			
			if (
					errorCode != 2104 &&	// Market data farm connection is OK  (we don't care about about market data in RefAPI)   
					errorCode != 2106		// HMDS data farm connection is OK:ushmds
			) {
				S.out( "Received API message  id=%s  errCode=%s  %s", id, errorCode, errorMsg);
			}
		}

		@Override public void show(String string) {
			S.out( "Show: " + string);
		}

		/** Simulate disconnect to test reconnect */
		public void disconnect() {
			m_controller.disconnect();
		}

		public void dump() {
			m_controller.dump();
		}

	}

	static String tos(OrderState orderState) {
		return String.format( "state=%s  initMargin=%s  ELV=%s  comm=%s", orderState.status().toString(), orderState.initMarginAfter(), orderState.equityWithLoanAfter(), orderState.commission() );
	}

	public static void require(boolean b, RefCode code, String errMsg, Object... params) throws RefException {
		// simulate failed error code?
		if (m_failCodes != null && "fail".equals( m_failCodes.get(code.toString() ) ) ) {
			b = false;
			S.out( "Force-failing " + code + " " + errMsg);
			errMsg += " (force-fail)";
		}
			// in random mode, 1 out of 8 calls will return an error
//			if (m_config.produceErrors().equals("random") ) {
//				int rnd = new Random(System.currentTimeMillis()).nextInt();
//				if (rnd % 8 == 1) b = false;
//			}
		if (!b) {
			throw new RefException( code, errMsg, params);
		}
	}

	/** Write to the log file. Don't throw any exception. */

	static void log( LogType type, String text, Object... params) {
		m_log.log( type, text, params);
	}

	static void log( String text) {
		m_log.log( text);
	}

	static class Pair {
		String m_key;
		String m_val;

		Pair( String key, String val) {
			m_key = key;
			m_val = val;
		}
	}

	@Override public void tradeReport(String tradeKey, Contract contract, Execution exec) {
		JsonObject obj = new JsonObject();
		obj.put( "time", exec.time() );         
		obj.put( "orderid", exec.orderId() );    
		obj.put( "permid", exec.permId() );    
		obj.put( "side", exec.side() );
		obj.put( "quantity", exec.shares().toDouble() ); 
		obj.put( "symbol", contract.symbol() );
		obj.put( "price", exec.price() );
		obj.put( "cumfill", exec.cumQty().toDouble() );
		obj.put( "conid", contract.conid() );
		obj.put( "exchange", exec.exchange() );
		obj.put( "avgprice", exec.avgPrice() );
		obj.put( "orderref", exec.orderRef() );
		obj.put( "tradekey", tradeKey);
		
		try {
			log( LogType.TRADE, obj.toString() );
			sqlConnection( conn -> conn.insertJson( "trades", obj) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** Ignore this. */
	@Override public void tradeReportEnd() {
	}

	@Override public void commissionReport(String tradeKey, CommissionReport rpt) {
		try {
			log( LogType.COMMISSION, "%s %s %s %s", rpt.execId(), rpt.commission(), rpt.currency(), tradeKey);

			Object[] vals = {
					tradeKey,
					rpt.commission(),
					rpt.currency()
			};

			sqlConnection( conn -> conn.insert( "commissions", vals) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** This creates a new connection every time. You could take approach like Redis where you keep it open */
	void sqlConnection(SqlRunnable runnable) throws Exception {
		m_config.sqlConnection(runnable);
	}

	public ApiController orderController() {
		return m_orderConnMgr.controller();
	}

	public ConnectionMgr orderConnMgr() {
		return m_orderConnMgr;
	}

	void dump() {
		S.out( "-----Dumping Stocks-----");
		JsonObject.display( m_stocks.stocks(), 0, false);
		
		S.out( "Dumping config");
		m_config.dump();
	}

	/** Used to query prices from Redis. */
	static class PriceQuery {
		Stock m_stock;
		private Response<Map<String, String>> m_res;  // returns a map of tag->val where tag =bid/ask/... and val is price

		public PriceQuery(Pipeline pipeline, Stock stock) {
			m_stock = stock;
			m_res = pipeline.hgetAll( conidStr() );
		}

		/** Update the stock from the prices in m_res;
		 *  Must be called after the pipeline is closed. */
		void updateStock() {
			m_stock.setPrices( new Prices(m_res.get() ) );
		}

		public String conidStr() {
			return (String)m_stock.get( "conid");
		}
	}

	public void queryAllPrices() {  // might want to move this into a separate microservice
		//S.out( "querying prices");

		try {
			// send a single query to Redis for the prices
			// the responses are fed into the PriceQuery objects
			ArrayList<PriceQuery> list = new ArrayList<PriceQuery>();
			
			m_redis.pipeline( pipeline -> {
				for (Object stock : m_stocks.stocks()) {
					list.add( new PriceQuery(pipeline, (Stock)stock) );
				}
			});
			
			// update the stock object in place       // synchronize this? pas
			for (PriceQuery priceQuery : list) {
				priceQuery.updateStock();
			}
		}
		catch( JedisException e) {
			m_log.log(LogType.JEDIS, e.getMessage() );
		}
		catch( Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, e.getMessage() );
		}
	}

	private void handleGetStocksWithPrices(HttpExchange exch) {
		new BackendTransaction(this, exch).respond( m_stocks.stocks());
	}

	/** This can be used to serve static json stored in a string
	 *  @param data must be in json format */
	private void quickResponse(HttpExchange exch, String data, int code) {
		new BackendTransaction(this, exch); // print out uri
		
		try (OutputStream outputStream = exch.getResponseBody() ) {
			exch.getResponseHeaders().add( "Content-Type", "application/json");
			exch.sendResponseHeaders( code, data.length() );
			outputStream.write(data.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while sending FAQ");
		}
	}

	/** this seems useless since you can still be left with .000001 */
	static double round(double val) {
		return Math.round( val * 100) / 100.;
	}
	
	JsonObject type2Config() {
		return m_type2Config;
	}

	public JsonArray hotStocks() {
		return m_stocks.hotStocks();
	}

	/** @param side is buy or sell (lower case) */
	boolean validWallet(String walletAddr, String side) {
		String allow = m_blacklist.getNN( walletAddr).toLowerCase();
		return S.isNull(allow) || allow.equals("all") || allow.equals(side); 
	}
	
}

//no change

// Issues
// high: put in a check if an order fills after a timeout; that's a WARNING and ALERT for someone to do something, or for the program to close out the position
// high: add a check for max value; add a test for it
// you must submit the order at the right price to ensure you get filled at least .4% profit, or whatever

// Bugs
// low: on Sunday night, at least, a what-if order returns all Double.max_value strings to api
// *probably more efficient to have one timer thread instead of one for each message; fix this when it gets busy
