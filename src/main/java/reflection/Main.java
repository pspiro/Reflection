package reflection;

import java.io.OutputStream;
import java.time.DayOfWeek;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JsonObject;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.OrderState;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.sun.net.httpserver.HttpExchange;

import chain.Allow;
import chain.Stocks;
import chain.Stocks.Stock;
import common.Alerts;
import common.ConnectionMgrBase;
import common.Util;
import http.BaseTransaction;
import http.MyClient;
import http.MyServer;
import reflection.Config.MultiChainConfig;
import reflection.MySqlConnection.SqlCommand;
import reflection.TradingHours.Session;
import siwe.SiweTransaction;
import test.MyTimer;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.LogType;

public class Main implements ITradeReportHandler {
	// constants
	enum Status { Connected, Disconnected };
	enum Time { Before, After, Unknown }

	public static final int DB_PAUSE = 50; // pause n ms before writing to db

	// no ticks in this time and we have a problem
	private static final long SmartInterval = Util.MINUTE * 2; 
	private static final long OvernightInterval = Util.MINUTE * 15; 

	// static
	private static final Random rnd = new Random( System.currentTimeMillis() );
	static final MultiChainConfig m_config = new MultiChainConfig();

	static GTable m_failCodes;  // table of error codes that we want to fail; used for testing, only read of Config.produceErrors is true
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started

	// member vars
	private final ConnectionMgr m_orderConnMgr; // we assume that TWS is connected to IB at first but that could be wrong; is there some way to find out?
	private final String m_tabName;
	private String m_faqs;
	private String m_type1Config; 
	private JsonObject m_type2Config;
	final TradingHours m_tradingHours; 
	private GTable m_blacklist;  // wallet is key, case insensitive
	private DbQueue m_dbQueue = new DbQueue();
	private String m_mdsUrl;  // the full query to get the prices from MdServer
	boolean m_staleMktData; // if true, we have likely stopped receiving market data from mdserver
    private Time m_timeWas = Time.Unknown;  // used for sending daily summary emails
    private final Stocks m_stocks = new Stocks();

	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("RefAPI");
			S.out( "Starting RefAPI - log times are NY time");
			
			new Main( args);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}

	public Main(String[] args) throws Exception {
		m_tabName = SingleChainConfig.getTabName( args);
		MyClient.restart( "refapi.http.log");
		
		MyTimer timer = new MyTimer();

		// read config settings from google sheet; this must go first since other items depend on it
		timer.next("Reading from google spreadsheet %s", m_tabName, NewSheet.Reflection);
		readSpreadsheet(true);
		timer.done();
		
		// must come after reading config and before writing to log
		Util.execute( "DBQ", () -> m_dbQueue.runDbQueue() );
		
		// create log entry
		jlog( LogType.RESTART, null, null, Util.toJson( 
				"buildTime", Util.readResource( Main.class, "version.txt") ), 0 );  // log build date/time

		// check database connection to make sure it's there
		timer.next( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		m_config.sqlCommand( conn -> {} );
		
		// confirm we can access private keys
		timer.next( "Checking access to keys");
		m_config.chains().checkKeys();

		// start price query thread
		timer.next( "Starting stock price query thread every n ms");
		Util.executeEvery( 0, m_config.mdQueryInterval(), () -> queryAllPrices() );
		
		// start Siwe thread to periodically save siwe cookies
		SiweTransaction.startThread();
		
		timer.next( "Creating http server");
		MyServer.listen( m_config.refApiPort(), m_config.threads(), server -> {
			//server.createContext("/favicon", exch -> quickResponse(exch, "", 200) ); // respond w/ empty response
			
			server.createContext("/tdxrefl/get-stocks", exch -> new BackendTransaction(this, exch, true).handleGetTdxStocks() );  // old code, obsolete, remove
			server.createContext("/tdxrefl/stock-details", exch -> new BackendTransaction(this, exch, true).handleTdxStockDetails() );  // old code, obsolete, remove

			// onramp
			server.createContext("/api/onramp", exch -> new BackendTransaction(this, exch, true).handleOnramp() );  // old code, obsolete, remove
			server.createContext("/api/onramp-get-quote", exch -> new OnrampTransaction( this, exch).handleGetQuote() );
			server.createContext("/api/onramp-convert", exch -> new OnrampTransaction( this, exch).handleConvert() );
			
			// old-style, used by test scripts
			server.createContext("/siwe/init", exch -> new SiweTransaction( exch).handleSiweInit() );
			server.createContext("/siwe/signin", exch -> new SiweTransaction( exch).handleSiweSignin() );
			server.createContext("/siwe/me", exch -> new SiweTransaction( exch).handleSiweMe() );
			server.createContext("/siwe/signout", exch -> new SiweTransaction( exch).handleSiweSignout() );

			// sent by frontend
			server.createContext("/api/siwe/init", exch -> new SiweTransaction( exch).handleSiweInit() );
			server.createContext("/api/siwe/signin", exch -> new SiweTransaction( exch).handleSiweSignin() );
			server.createContext("/api/siwe/me", exch -> new SiweTransaction( exch).handleSiweMe() );
			server.createContext("/api/siwe/signout", exch -> new SiweTransaction( exch).handleSiweSignout() );

			// orders and live orders
/*OK*/		server.createContext("/api/order", exch -> new OrderTransaction(this, exch).backendOrder() );
			server.createContext("/api/working-orders", exch -> new LiveOrderTransaction(this, exch, false).handleGetLiveOrders() ); // remove after frontend migrates to live-orders. pas
			server.createContext("/api/live-orders", exch -> new LiveOrderTransaction(this, exch, false).handleGetLiveOrders() );
			server.createContext("/api/clear-live-orders", exch -> new LiveOrderTransaction(this, exch, true).clearLiveOrders() );
			server.createContext("/api/fireblocks", exch -> new LiveOrderTransaction(this, exch, true).handleFireblocks() ); // report build date/time
			server.createContext("/api/all-live-orders", exch -> new LiveOrderTransaction(this, exch, true).handleAllLiveOrders() );  // used by Monitor only

			// get/update profile
			server.createContext("/api/get-profile", exch -> new ProfileTransaction(this, exch).handleGetProfile() );
			server.createContext("/api/update-profile", exch -> new ProfileTransaction(this, exch).handleUpdateProfile() );
			server.createContext("/api/validate-email", exch -> new ProfileTransaction(this, exch).validateEmail() );
			server.createContext("/api/users/register", exch -> new BackendTransaction(this, exch).handleRegister() ); // kyc/persona completed
			server.createContext("/api/check-identity", exch -> new BackendTransaction(this, exch).checkIdentity() );
			server.createContext("/api/fund-wallet", exch -> new BackendTransaction(this, exch).handleFundWallet() );
			
			// get/set config
			server.createContext("/api/system-configurations/last", exch -> quickResponse(exch, m_type1Config, 200) );// we can do a quick response because we already have the json; requested every 30 sec per client; could be moved to nginx if desired
			server.createContext("/api/configurations", exch -> new BackendTransaction(this, exch, false).handleGetType2Config() );
			server.createContext("/api/faqs", exch -> quickResponse(exch, m_faqs, 200) );

			// dashboard panels
			server.createContext("/api/crypto-transactions", exch -> new BackendTransaction(this, exch, false).handleReqCryptoTransactions(exch) ); // obsolete, have frontend remove this
			server.createContext("/api/transactions", exch -> new BackendTransaction(this, exch, false).handleReqCryptoTransactions(exch) );
			server.createContext("/api/mywallet", exch -> new BackendTransaction(this, exch, false).handleMyWallet() );
			server.createContext("/api/show-faucet", exch -> new BackendTransaction(this, exch).handleShowFaucet() );
			server.createContext("/api/turn-faucet", exch -> new BackendTransaction(this, exch).handleTurnFaucet() );
			server.createContext("/api/positions-new", exch -> new PortfolioTransaction(this, exch, false).handleReqPositionsNew() ); // for My Reflection panel
			server.createContext("/api/redemptions/redeem", exch -> new RedeemTransaction(this, exch).handleRedeem() );

			// get stocks and prices
			server.createContext("/api/hot-stocks", exch -> new BackendTransaction(this, exch, false).handleHotStocks() );  // hot stocks for home page
			server.createContext("/api/get-watch-list", exch -> new BackendTransaction(this, exch, false).handleWatchList() );  // watch list for Dashboard
			server.createContext("/api/get-all-stocks", exch -> new BackendTransaction(this, exch).handleAllStocks() );  // all stocks for dropdown on trade page
			server.createContext("/api/get-price", exch -> new BackendTransaction(this, exch, false).handleGetPrice() );  // Frontend calls this, I think for price on Trading screen
			server.createContext("/api/trading-screen-dynamic", exch -> new BackendTransaction(this, exch).handleTradingDynamic() );
			
			// status
			server.createContext("/api/user-token-mgr", exch -> new BackendTransaction(this, exch).handleUserTokenMgr() ); // used by Monitor only
			server.createContext("/api/reset-user-token-mgr", exch -> new BackendTransaction(this, exch).resetUserTokenMgr() );
			server.createContext("/api/get-position-tracker", exch -> new OrderTransaction(this, exch).getPositionTracker() ); // used by Monitor for debugging
			server.createContext("/api/debug-on", exch -> new BackendTransaction(this, exch).handleDebug(true) );
			server.createContext("/api/debug-off", exch -> new BackendTransaction(this, exch).handleDebug(false) );
			server.createContext("/api/about", exch -> new BackendTransaction(this, exch).about() ); // report build date/time; combine this with status
			server.createContext("/api/status", exch -> new BackendTransaction(this, exch).handleStatus() );
			server.createContext("/api/ok", exch -> new BaseTransaction(exch, false).respondOk() ); // this is sent every couple of seconds by Monitor
			server.createContext("/api/myip", exch -> new BackendTransaction(this, exch).handleMyIp() );
			server.createContext("/api", exch -> new OldStyleTransaction(this, exch).handle() );
			server.createContext("/", exch -> new BaseTransaction(exch, true).respondNotFound() );

			// landing page
			server.createContext("/api/signup", exch -> new BackendTransaction(this, exch).handleSignup() );
			server.createContext("/api/log", exch -> new BackendTransaction(this, exch).handleLog() );
			server.createContext("/api/sag", exch -> new BackendTransaction(this, exch).handleSagHtml() );
			server.createContext("/api/contact", exch -> new BackendTransaction(this, exch).handleContact() );

			// obsolete, remove
			server.createContext("/api/trading-screen-static", exch -> new BackendTransaction(this, exch).handleTradingStatic() );
			server.createContext("/api/get-stock-with-price", exch -> new BackendTransaction(this, exch, false).handleGetStockWithPrice() ); // from trade page in prod only
			server.createContext("/api/users/wallet", exch -> new BackendTransaction(this, exch, false).respondOk() );   // obsolete, remove this
			server.createContext("/api/system-configurations", exch -> quickResponse(exch, "Query not supported", 400) );

		});

		m_orderConnMgr = new ConnectionMgr( m_config.twsOrderHost(), m_config.twsOrderPort(), m_config.twsOrderClientId() );
		m_tradingHours = new TradingHours(orderController(), m_config); // must come after ConnectionMgr 

		// connect to TWS
		timer.next( "Connecting to TWS on %s:%s", m_config.twsOrderHost(), m_config.twsOrderPort() );
		m_orderConnMgr.startTimer();  // ideally we would set a timer to make sure we get the nextId message
		timer.done();
		
		Runtime.getRuntime().addShutdownHook(new Thread( this::shutdown) );
		
		// check market data every minute (production only)
		if (!Main.m_config.checkStaleData() ) {
			S.out( "checking for stale mkt data every minute");
			Util.executeEvery( Util.MINUTE, Util.MINUTE, this::checkMktData);
		}

		// send out daily account summaries
		if (m_config.isProduction() && m_config.maxSummaryEmails() > 0) {
			Util.executeEvery( Util.MINUTE, Util.MINUTE, this::checkSummaries);
		}
		
		Util.executeEvery( Util.MINUTE, Util.MINUTE, this::checkHookServers);
		Util.executeEvery( Util.DAY, Util.DAY, this::checkAdminBalance);
	}

	void shutdown() {
		log( LogType.SHUTDOWN, null);
	}

	void readSpreadsheet(boolean readStocks) throws Exception {
		Book book = NewSheet.getBook(NewSheet.Reflection);
		
		// read RefAPI config
		m_config.readFromSpreadsheet( book, m_tabName );  // must go first
		m_config.chains().useAdmin1();
		m_stocks.readFromSheet( book);

		// read Backend config (used by Frontend)
		readFaqsFromSheet(book);
		m_type1Config = readType1Config(book).toString();
		m_type2Config = readConfig(book, 2);

		// read list of RefCodes where we want to simulate failure
		m_failCodes = S.isNotNull( m_config.errorCodesTab() )
			? new GTable( book.getTab(m_config.errorCodesTab()), "Code", "Fail", true)
			: null;
		
		m_blacklist = new GTable( book.getTab("Blacklist"), "Wallet Address", "Allow", false);
		m_mdsUrl = String.format( "%s/mdserver/get-ref-prices", m_config.mdsConnection() );

	}

	/** as of 5/10/24 Frontend no longer needs buy_spread and sell_spread; the spreads
	 *  are now incorporated into the prices we send on the dynamic trading page query;
	 *  these tags can/should be removed after frontend is promoted to prod */ 
	private String readType1Config(Book book) throws Exception {
		JsonObject obj = new JsonObject();
		for (String key : "min_order_size,max_order_size,non_kyc_max_order_size,price_refresh_interval,commission,buy_spread,sell_spread".split(",") ) {
			obj.put( key, m_config.getRequiredDouble(key) );
		}
		return obj.toString();
	}

	/** FAQs are returned directly from nginx; this is just a stub */ 
	void readFaqsFromSheet(Book book) throws Exception {
		m_faqs = """
				[ { "question": "What is Reflection?", "answer": "Reflection is the only platform where you can buy and sell stock tokens that are 100% backed by shares of real stock." }, { "question": "What are stock tokens?", "answer": "Stock tokens are crypto tokens which represent shares of stock in publically traded companies." } ]""";
	}

	/** You could shave 300 ms by sharing the same Book as Config 
	 * @param book */ 
	JsonObject readConfig(Book book, int type) throws Exception {   // remove the type parameter after 9/9/23 and read all entries and delete type-1 entries from Jitin-config
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
	
	Stock getStock( int conid) throws Exception {
		Stock stock = m_stocks.getStockByConid( conid);
		require(stock != null, RefCode.NO_SUCH_STOCK, "Unknown conid %s", conid);
		return stock;
	}


	/** Manage the connection from this client to TWS. */
	class ConnectionMgr extends ConnectionMgrBase {
		private static int clientId(int clientId) {
			return clientId == 0 
					? rnd.nextInt( Integer.MAX_VALUE) + 1
					: clientId;
		}

		/** If clientId is zero, pick a random one */
		ConnectionMgr(String host, int port, int clientId) {
			super( host, port, clientId( clientId), m_config.reconnectInterval() ); 

			m_controller.handleExecutions( Main.this);
		}

		/** Called when we receive server version. We don't always receive nextValidId. */
		@Override public void onConnected() {
			super.onConnected();  // stop the connection timer
			S.out( "TWS_CONNECTION: connected");
			
			m_tradingHours.startQuery();
		}

		/** Ready to start sending messages. */
		@Override public synchronized void onRecNextValidId(int id) {
			S.out( "TWS_CONNECTION: recNextValidId " + id);
		}

		@Override public synchronized void onDisconnected() {
			if (m_timer == null) {
				S.out( "TWS_CONNECTION: disconnected");
				startTimer();
			}
		}

		@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
			super.message( id, errorCode, errorMsg, advancedOrderRejectJson);
			
			if (
					errorCode != 2104 &&	// Market data farm connection is OK  (we don't care about about market data in RefAPI)   
					errorCode != 2106		// HMDS data farm connection is OK:ushmds
			) {
				S.out( "Received API message  id=%s  errCode=%s  %s", id, errorCode, errorMsg);
			}
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

	void log( LogType type, String text) {
		jlog( type, "-", "-", Util.toJson( "text", text), 0 );
	}

	/** Writes entry to log table in database; must not throw exception;
	 * @param uid may be null 
	 * @param chainId TODO*/
	void jlog( LogType type, String uid, String wallet, JsonObject json, int chainId) {
		S.out( "%sLogType.%s %s %s %s", 
				uid != null ? uid + " " : "",
				type,
				wallet,
				chainId,
				json);
		
		JsonObject log = Util.toJson(
				"type", type,
				"uid", uid,
				"wallet_public_key", wallet,
				"data", json,
				"chainId", chainId);
		
		queueSql( conn -> conn.insertJson( "log", log) );
	}

	@Override public void tradeReport(String tradeKey, Contract contract, Execution exec) {
		JsonObject obj = new JsonObject();
		obj.putIf( "time", exec.time() );         
		obj.putIf( "order_id", exec.orderId() );    
		obj.putIf( "perm_id", exec.permId() );    
		obj.putIf( "side", exec.side() );
		obj.putIf( "quantity", exec.shares().toDouble() ); 
		obj.putIf( "symbol", contract.symbol() );
		obj.putIf( "price", exec.price() );
		obj.putIf( "cumfill", exec.cumQty().toDouble() );
		obj.putIf( "conid", contract.conid() );
		obj.putIf( "exchange", exec.exchange() );
		obj.putIf( "avgprice", exec.avgPrice() );
		obj.putIf( "orderref", exec.orderRef() ); // this is the uid
		obj.putIf( "tradekey", tradeKey);

		// insert trade into trades and log tables; it's not urgent, this
		// table is never read, we can delay it
		queueSql( conn -> conn.insertJson( "trades", obj) );
		
//		try {
//			m_config.sqlCommand( conn -> conn.insertJson( "trades", obj) );
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		jlog( LogType.TRADE, 
				Util.left( exec.orderRef(), 8),  // order ref might hold more than 8 chars, e.g. "ABCDABCD unwind" 
				null, obj, 0);  
	}

	/** Ignore this. */
	@Override public void tradeReportEnd() {
	}

	@Override public void commissionReport(String tradeKey, CommissionReport rpt) {
		try {
			jlog( LogType.COMMISSION, null, null, Util.toJson( 
					"execId", rpt.execId(), 
					"commission", rpt.commission(), 
					"tradeKey", tradeKey), 0 );

			JsonObject obj = Util.toJson( 
					"tradekey", tradeKey,
					"comm_paid", rpt.commission() );

			queueSql( conn -> conn.insertJson( "commissions", obj) );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Run the sql command in the DbQueue thread */
	void queueSql(SqlCommand runnable) {  // could be status
		m_dbQueue.add(runnable);
	}

	public ApiController orderController() {
		return m_orderConnMgr.controller();
	}

	public ConnectionMgr orderConnMgr() {
		return m_orderConnMgr;
	}

	void dump() throws Exception {
		S.out( "-----Dumping Stocks-----");
		m_stocks.show();
		
		S.out( "Dumping config");
		m_config.dump();
	}

	/** query price from mdserver */
	public void queryAllPrices() {  // might want to move this into a separate microservice
		try {
			for (var prices : MyClient.getArray( m_mdsUrl) ) {				
				Stock stock = stocks().getStockByConid( prices.getInt("conid") );
				if (stock != null) {
					stock.setPrices( new Prices(prices) );
				}
			}
		}
		catch( Exception e) {
			S.err( "Error fetching prices", e); // need this because the exception doesn't give much info
		}
	}

	/** This can be used to serve static json stored in a string
	 *  @param data must be in json format */
	private void quickResponse(HttpExchange exch, String data, int code) {
		//new BaseTransaction(exch, true); // don't print out uri
		
		try (OutputStream outputStream = exch.getResponseBody() ) {
			exch.getResponseHeaders().add( "Content-Type", "application/json");
			exch.sendResponseHeaders( code, data.length() );
			outputStream.write(data.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
			log( LogType.SOCKET_ERROR, "Exception while sending FAQ");
		}
	}

	/** this seems useless since you can still be left with .000001 */
	public static double round(double val) {
		return Math.round( val * 100) / 100.;
	}
	
	JsonObject type2Config() {
		return m_type2Config;
	}

	/** @param side is buy or sell (lower case) */
	boolean validWallet(String walletAddr, Action side) {
		String str = m_blacklist.get(walletAddr);
		return Util.getEnum(str, Allow.values(), Allow.All).allow(side);
	}

	/** called every one minute in the Util thread to check for stale market data*/
	private void checkMktData() {
		Util.wrap( () -> {
			if (stale() != m_staleMktData) {
				m_staleMktData = !m_staleMktData;
				
				Alerts.alert("RefAPI", "STALE MARKET DATA: " + m_staleMktData, "");
			}
		});
	}

	/** return true if all stocks are stale, indicating lack of market data 
	 * @throws Exception */ 
	boolean stale() throws Exception {
		long latest = m_stocks.getLatest();
		long interval = System.currentTimeMillis() - latest ;
		Session session = m_tradingHours.getTradingSession( true, "");
		return session == Session.Smart && interval > SmartInterval ||
			   session == Session.Overnight && interval > OvernightInterval;
	}


	/** This class processes database queries in a separate thread so as not
	 *  to hold up other threads. It waits for the first query and then processes
	 *  as many as possible, then waits again. */
	class DbQueue {
		private LinkedBlockingQueue<SqlCommand> m_queue = new LinkedBlockingQueue<>();

		void add(SqlCommand command) {
			m_queue.add(command);
		}
		
		// if this is still too slow, you can keep a connection open, like for redis

		/** Runs in a separate thread to execute database commands without holding
		 *  up the TWS thread */
		private void runDbQueue() {
			try {
				while (true) {
					// wait for the first one
					SqlCommand com = m_queue.take();
					//S.sleep(DB_PAUSE);  // you could sleep a little to try to batch more entries, but connecting takes 200ms anyway

					// then connect and process as many as possible
					try ( MySqlConnection conn = m_config.createConnection() ) {
						while (com != null) {
							try {
								com.run( conn);   // (wrap() doesn't work here)
							}
							catch( Exception e) {
								S.err( "Error while executing DbQueue command", e);
								e.printStackTrace();  // this would be an error on one specific database operation
							}
							catch( Throwable e) { // you would come here for e.g. StackOverflowError
								S.out( "Bad error while executing DbQueue command");
								e.printStackTrace();  // this would be an error on one specific database operation
							}
							com = next();
						}
					} 
					catch (Exception e) {
						S.err( "Error while connecting to database", e);
						e.printStackTrace();  // this could be an error connecting to the database; ideally we would put the command, which didn't execute, back in the queue
					}
				}
			}
			catch( Throwable e) {  // if it comes here, fix the actual culprit
				S.out( "Fatal error: DbQueue thread was interrupted");
				e.printStackTrace();
			}
		}
		
		private SqlCommand next() {
			return m_queue.isEmpty() ? null : m_queue.remove();
		}
	}

    /** check if it's time to send out the summary emails; when data changes in NY
     *  currently sending summaries only for Polygon */
	void checkSummaries() {
		boolean nowAfter = Util.isLaterThanEST( 16); // 4 pm

		// if we just passed the time threshold...
		if (m_timeWas == Time.Before && nowAfter) {
            DayOfWeek dayOfWeek = Util.getDayEST();

            // and not a weekend, send the daily email summaries
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
            	Util.executeAndWrap( () -> new Statements( m_config, m_config.chains().polygon(), false, m_stocks) // don't tie up the Util timer thread
            			.generateSummaries( m_config.maxSummaryEmails() ) );
        	}
        }
		
		m_timeWas = nowAfter ? Time.After : Time.Before;
	}

	
	/** you can use this for anything price-related, but not blockchain related 
	 * @return */
	protected Stocks stocks() {
		return m_stocks;
	}
	
	private void checkHookServers() {
		Util.wrap( () -> {
			for (var chain : m_config.chains().values() ) {
				var json = MyClient.getJson( chain.params().getWebhookUrl() );
				if (!json.getString( "code").equals( "OK") ) {
					Alerts.alert("RefAPI", "WARNING! HookServer not responding!", json.getString( "name") );
				}
			}
		});
	}

	/** send an alert if admin wallet is running out of gas; it's really bad if that happens */
	private void checkAdminBalance() {
		try {
			m_config.chains().checkAdminBalance();
		}
		catch( Exception e) {
			e.printStackTrace();
			Alerts.alert( "RefAPI", "WARNING: ADMIN WALLET IS RUNNING OUT OF GAS", e.getMessage() );
		}
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

// NOTES:
// If Transaction.wrap() catches, the message is considered failed
// If Util.wrap() catches, it prints the error but the message handling continues