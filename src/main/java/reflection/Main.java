package reflection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.OrderState;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fireblocks.Accounts;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.Transfer;
import json.MyJsonObject;
import redis.MyRedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;
import reflection.Config.RefApiConfig;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.MyException;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

public class Main implements HttpHandler, ITradeReportHandler {
	enum Status {
		Connected, Disconnected
	};
	
	private static final Random rnd = new Random( System.currentTimeMillis() );
	static final Config m_config = new RefApiConfig();
	static final MySqlConnection m_database = new MySqlConnection();
	private static DateLogFile m_log = new DateLogFile("reflection"); // log file for requests and responses

	private final MyRedis m_redis;  // used for periodically querying the prices 
	private final HashMap<Integer,Stock> m_stockMap = new HashMap<Integer,Stock>(); // map conid to JSON object storing all stock attributes; prices could go here as well if desired. pas
	private final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final ConnectionMgr m_orderConnMgr; // we assume that TWS is connected to IB at first but that could be wrong; is there some way to find out?
	private final String m_tabName;
	
	JSONArray stocks() { return m_stocks; }

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
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	public Main(String tabName) throws Exception {
		// create log file folder and open log file
		log( LogType.RESTART, Util.readResource( Main.class, "version.txt") );  // print build date/time

		// read config settings from google sheet
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		m_tabName = tabName;
		S.out( "  done");
		
		if (m_config.useFireblocks() ) {
			Accounts.instance.setAdmins( "Admin1,Admin2");  // better to pull from config or just use Admin*
		}

		// APPROVE-ALL SETTING IS DANGEROUS and not normal
		// make user approve it during startup
		if (m_config.autoFill() ) {
			S.out( "WARNING: The RefAPI will approve all orders and WILL NOT SEND ORDERS TO THE EXCHANGE");
//			if (!S.input( "Are you sure? (yes/no)").equals( "yes") ) {
//				return;
//			}
		}
		
		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");

		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
		S.out( "  done");

		// if port is zero, host contains connection string, otherwise host and port are used
		if (m_config.redisPort() == 0) {
			S.out( "Connecting to redis with connection %s", m_config.redisHost() );
			Util.require( JedisURIHelper.isValid( URI.create(m_config.redisHost() ) ), "redis connect string is invalid" );
			m_redis = new MyRedis(m_config.redisHost() );
		}
		else {
			S.out( "Connecting to redis server on %s:%s", m_config.redisHost(), m_config.redisPort() );
			m_redis = new MyRedis(m_config.redisHost(), m_config.redisPort() );
		}
		m_redis.connect(); // this is not required but we want to bail out if redis is not running
		S.out( "  done");

		S.out( "Starting stock price query thread every n ms");
		Util.executeEvery( m_config.redisQueryInterval(), () -> queryAllPrices() );  // improve this, set up redis stream

		S.out( "Listening on %s:%s  (%s threads)", m_config.refApiHost(), m_config.refApiPort(), m_config.threads() );
		HttpServer server = HttpServer.create(new InetSocketAddress(m_config.refApiHost(), m_config.refApiPort() ), 0);
		//HttpServer server = HttpServer.create(new InetSocketAddress( m_config.refApiPort() ), 0);
		server.createContext("/favicon", Util.nullHandler); // ignore these requests
		server.createContext("/mint", exch -> handleMint(exch) );
		server.createContext("/api/reflection-api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/reflection-api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/reflection-api/get-stock-with-price", exch -> handleGetStockWithPrice(exch) );
		server.createContext("/api/reflection-api/get-price", exch -> handleGetPrice(exch) );
		server.createContext("/api/reflection-api/order", exch -> handleOrder(exch) );
		server.createContext("/api/reflection-api/positions", exch -> handleReqTokenPositions(exch) );		
		server.createContext("/api/redemptions/redeem", exch -> handleRedeem(exch) );
		server.createContext("/siwe/init", exch -> new SiweTransaction( this, exch).handleSiweInit() );
		server.createContext("/siwe/signin", exch -> new SiweTransaction( this, exch).handleSiweSignin() );
		server.createContext("/siwe/signout", exch -> new SiweTransaction( this, exch).handleSiweSignout() );
		server.createContext("/siwe/me", exch -> new SiweTransaction( this, exch).handleSiweMe() );
		server.createContext("/", this);
		server.setExecutor( Executors.newFixedThreadPool(m_config.threads()) );  // multiple threads but we are synchronized for single execution
		server.start();
		S.out( "  done");

		// connect to TWS
		m_orderConnMgr = new ConnectionMgr( m_config.twsOrderHost(), m_config.twsOrderPort() );
		m_orderConnMgr.connectNow();  // ideally we would set a timer to make sure we get the nextId message
		S.out( "  done");

		Runtime.getRuntime().addShutdownHook(new Thread( () -> log(LogType.TERMINATE, "Received shutdown msg from linux kill command")));
	}

	/** Refresh list of stocks and re-request market data. */
	void refreshStockList() throws Exception {
		m_stocks.clear();
		readStockListFromSheet();
	}
	
	public static HashMap<Integer, ListEntry> readMasterSymbols() throws Exception {
		HashMap<Integer,ListEntry> map = new HashMap<>();
		for (ListEntry entry : NewSheet.getTab( NewSheet.Reflection, "Master-symbols").fetchRows(false) ) {
			map.put( entry.getInt("Conid"), entry);
		}
		return map;
	}

	// let it fall back to read from a flatfile if this fails. pas
	@SuppressWarnings("unchecked")
	private void readStockListFromSheet() throws Exception {
		// read master list of symbols and map conid to entry
		HashMap<Integer,ListEntry> map = readMasterSymbols();
		
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, m_config.symbolsTab() ).fetchRows(false) ) {
			Stock stock = new Stock();
			if ("Y".equals( row.getValue( "Active") ) ) {
				int conid = Integer.valueOf( row.getValue("Conid") );

				stock.put( "conid", String.valueOf( conid) );
				stock.put( "smartcontractid", row.getValue("TokenAddress") );
				
				ListEntry masterRow = map.get(conid);
				Util.require( masterRow != null, "No entry in Master-symbols for conid " + conid);
				stock.put( "symbol", masterRow.getValue("Symbol") );
				stock.put( "description", masterRow.getValue("Description") );
				stock.put( "type", masterRow.getValue("Type") ); // Stock, ETF, ETF-24
				stock.put( "exchange", masterRow.getValue("Exchange") );

				m_stocks.add( stock);
				m_stockMap.put( conid, stock);
			}
		}
	}

	String getExchange( int conid) throws RefException {
		return getStock(conid).getString("exchange");
	}

	String getSmartContractId( int conid) throws RefException {
		return getStock(conid).getString("smartcontractid");
	}

	String getType( int conid) throws RefException {
		return getStock(conid).getString("type");
	}

	Stock getStock( int conid) throws RefException {
		Stock stock = m_stockMap.get( conid);
		require(stock != null, RefCode.NO_SUCH_STOCK, "Unknown conid %s", conid);
		return stock;
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

			S.out( "Received API error  id=%s  errCode=%s  %s", id, errorCode, errorMsg);
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

	/** Handle HTTP msg synchronously */
	@Override public synchronized void handle(HttpExchange exch) throws IOException {  // we could/should reduce the amount of synchronization, especially if there are messages that don't require the API
		new MyTransaction( this, exch).handle();
	}

	static String tos(OrderState orderState) {
		return String.format( "state=%s  initMargin=%s  ELV=%s  comm=%s", orderState.status().toString(), orderState.initMarginAfter(), orderState.equityWithLoanAfter(), orderState.commission() );
	}

	public static void require(boolean b, RefCode code, String errMsg, Object... params) throws RefException {
		// in test mode, 1 out of 8 calls will return an error
		if (m_config.produceErrors() ) {
			int rnd = new Random(System.currentTimeMillis()).nextInt();
			if (rnd % 8 == 1) b = false;
		}
		if (!b) {
			throw new RefException( code, errMsg, params);
		}
	}

	/** Write to the log file. Don't throw any exception. */

	static void log( LogType type, String text, Object... params) {
		m_log.log( type, text, params);
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
		// WARNING: you cannot change the order of these
		Object[] dbValues = {
				exec.time(),
				exec.orderId(),
				exec.side(),
				exec.shares().toDouble(),
				contract.symbol(),
				exec.price(),
				exec.permId(),
				exec.cumQty(),
				contract.conid(),
				exec.exchange(),
				exec.avgPrice(),
				exec.orderRef(),
				tradeKey
			};

		Object[] msgValues = {
				exec.orderId(),
				exec.side(),
				exec.shares().toDouble(),
				contract.symbol(),
				contract.conid(),
				exec.price(),
				exec.exchange(),

				exec.permId(),
				exec.cumQty(),
				exec.avgPrice(),
				exec.orderRef(),
				tradeKey
			};

		try {
			log( LogType.TRADE, "id=%s  %s %s shares of %s (%s) at %s on %s  %s %s %s %s %s", msgValues);
			m_database.insert( "trades", dbValues);
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

			m_database.insert( "commissions", vals);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ApiController orderController() {
		return m_orderConnMgr.controller();
	}

	public ConnectionMgr orderConnMgr() {
		return m_orderConnMgr;
	}

	public String tabName() {
		return m_tabName;
	}

	void dump() {
		S.out( "-----Dumping Stocks-----");
		MyJsonObject.display( m_stocks, 0, false);
		
		S.out( "Dumping config");
		m_config.dump();
	}

	/** This returns json tags of bid/ask but it might be returning other prices if bid/ask is not available. */
	Prices getPrices(int conid) throws JedisException {
		// create a new redis connection; this is not as fast as if we keep the connection open
		// if you do that, make sure to synchronize the calls, and don't share w/ the existing MyRedis
		// because you get: Cannot use Jedis when in Pipeline. Please use Pipeline or reset jedis state.
		return new Prices( 
				m_config.createJedis().hgetAll( String.valueOf(conid) ) 
		);
	}

	public void handleMint(HttpExchange exchange) throws IOException {
		String response;

		try {
			String uri = getURI(exchange);
			Util.require( uri.length() < 4000, "URI is too long");

			String[] parts = uri.split("/");
			Util.require( parts.length == 3, "Format of URL should be https://reflection.trade/mint/0x...  where the last piece of the URL is your wallet address");

			mint( parts[2]);
			response = m_config.mintHtml();
		}
		catch (Exception e) {
			e.printStackTrace();
			response = "An error occurred - " + e.getMessage();
		}

		//String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());
		try (OutputStream outputStream = exchange.getResponseBody() ) {
			exchange.getResponseHeaders().add( "Content-Type", "text/html");
			exchange.sendResponseHeaders( 200, response.length() );
			outputStream.write( response.getBytes() );
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.ERROR, "Exception while responding with html");
		}
	}

	/** Transfer some BUSD and ETH to the user's wallet */
	static void mint( String dest) throws Exception {
		Util.require(dest.length() == 42, "The wallet address is invalid");

		S.out( "Transferring %s BUSD to %s", m_config.mintBusd(), dest);
		String id1 = Transfer.transfer( Fireblocks.testBusd, 1, dest, m_config.mintBusd(), "Transfer BUSD");
		S.out( "  FB id is %s", id1);

		S.out( "Transferring %s Goerli ETH to %s", m_config.mintEth(), dest);
		String id2 = Transfer.transfer( Fireblocks.platformBase, 1, dest, m_config.mintEth(), "Transfer ETH");
		S.out( "  FB id is %s", id2);

		log( LogType.MINT, "Minted to %s", dest);
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
				for (Object stock : m_stocks) {
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

	// frontend expects an error msg like this
//		{
//		"statusCode": 400,
//		"message": "Bad Request"
//		}


	private void handleGetStocksWithPrices(HttpExchange exch) {
		getURI(exch);
		new BackendTransaction(this, exch).respond( m_stocks);
	}

	private void handleReqTokenPositions(HttpExchange exch) {
		String uri = getURI(exch);
		new BackendTransaction(this, exch).handleReqPositions(uri);
	}

	private void handleGetStockWithPrice(HttpExchange exch) {
		String uri = getURI(exch);
		new BackendTransaction(this, exch).handleGetStockWithPrice( uri);
	}

	private void handleGetPrice(HttpExchange exch) {
		String uri = getURI(exch);
		new BackendTransaction(this, exch).handleGetPrice( uri);
	}

	private void handleOrder(HttpExchange exch) {
		getURI(exch);
		new OrderTransaction(this, exch).backendOrder();
	}

	private void handleRedeem(HttpExchange exch) {
		String uri = getURI(exch);
		new BackendTransaction(this, exch).handleRedeem(uri);
	}
	
	/** Note this returns URI in all lower case */
	private static String getURI(HttpExchange exch) {
		String uri = exch.getRequestURI().toString().toLowerCase();
		S.out( "Handling %s", uri);
		return uri;
	}

	/** this seems useless since you can still be left with .000001 */
	static double round(double val) {
		return Math.round( val * 100) / 100.;
	}

	// VERY BAD AND INEFFICIENT. ps
	public HashMap getStockByTokAddr(String addr) throws RefException {
		require(Util.isValidAddress(addr), RefCode.INVALID_REQUEST, "Invalid address %s when getting stock by tok addr", addr);
		
		for (Object obj : m_stocks) {
			HashMap stock = (HashMap)obj;
			if ( ((String)stock.get("smartcontractid")).equalsIgnoreCase(addr) ) {
				return stock;
			}
		}
		return null;
	}
}



// Issues
// high: put in a check if an order fills after a timeout; that's a WARNING and ALERT for someone to do something, or for the program to close out the position
// high: add a check for max value; add a test for it
// you must submit the order at the right price to ensure you get filled at least .4% profit, or whatever

// Bugs
// low: on Sunday night, at least, a what-if order returns all Double.max_value strings to api
// *probably more efficient to have one timer thread instead of one for each message; fix this when it gets busy
