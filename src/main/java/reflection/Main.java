package reflection;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
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

import fireblocks.Fireblocks;
import json.MyJsonObject;
import json.StringJson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import reflection.MyTransaction.JRun;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

public class Main implements HttpHandler, ITradeReportHandler {
	enum Status { 
		Connected, Disconnected 
	};

	private final static Random rnd = new Random( System.currentTimeMillis() );
	final static Config m_config = new Config();
	final static MySqlConnection m_database = new MySqlConnection();
	private Jedis m_jedis;
	private final HashMap<Integer,StringJson> m_stockMap = new HashMap<Integer,StringJson>(); // map conid to JSON object storing all stock attributes; prices could go here as well if desired. pas 
	private final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final OrderConnectionMgr m_orderConnMgr = new OrderConnectionMgr(); // we assume that TWS is connected to IB at first but that could be wrong; is there some way to find out?
	private static DateLogFile m_log = new DateLogFile("reflection"); // log file for requests and responses
	private static boolean m_simulated;
	private String m_tabName;
	
	static boolean simulated() { return m_simulated; }

	JSONArray stocks() { return m_stocks; }
	
	public static void main(String[] args) {
		try {
			Fireblocks.setTestVals(); // readKeys();
			
			String configTab = null;
			for (String arg : args) {
				if (arg.equals( "simulated")) {
					m_simulated = true;
					S.out( "Running in simulated mode");
				}
				else {
					configTab = arg;
				}
			}
			
			if (S.isNull( configTab) ) {
				throw new Exception( "You must specify a config tab name");
			}
			
			new Main().run( configTab);
		}
		catch( BindException e) {
			S.out( "The application is already running");
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void run(String tabName) throws Exception {
		// create log file folder and open log file
		log( LogType.RESTART, Util.readResource( Main.class, "version.txt") );  // print build date/time

		// read config settings from google sheet 
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		m_tabName = tabName;
		S.out( "  done");

		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
		S.out( "  done");
		
		S.out( "Connecting to redis server on %s port %s", m_config.redisHost(), m_config.redisPort() );
		m_jedis = new Jedis(m_config.redisHost(), m_config.redisPort() );
		m_jedis.get( "test");
		S.out( "  done");

		S.out( "Listening on %s:%s  (%s threads)", m_config.refApiHost(), m_config.refApiPort(), m_config.threads() );
		HttpServer server = HttpServer.create(new InetSocketAddress(m_config.refApiHost(), m_config.refApiPort() ), 0);
		//HttpServer server = HttpServer.create(new InetSocketAddress( m_config.refApiPort() ), 0);
		server.createContext("/favicon", Util.nullHandler); // ignore these requests
		server.createContext("/", this); 
		server.setExecutor( Executors.newFixedThreadPool(m_config.threads()) );  // multiple threads but we are synchronized for single execution
		server.start();
		S.out( "  done");

		// connect to TWS
		m_orderConnMgr.connect( m_config.twsOrderHost(), m_config.twsOrderPort() );
	}

	/** Refresh list of stocks and re-request market data. */ 
	void refreshStockList() throws Exception {
		m_stocks.clear();
		readStockListFromSheet();
	}

	// let it fall back to read from a flatfile if this fails. pas
	@SuppressWarnings("unchecked")
	private void readStockListFromSheet() throws Exception {
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, m_config.symbolsTab() ).fetchRows(false) ) {
			StringJson stock = new StringJson();
			if ("Y".equals( row.getValue( "Active") ) ) {
				int conid = Integer.valueOf( row.getValue("Conid") );
				
				stock.put( "symbol", row.getValue("Symbol") );
				stock.put( "conid", String.valueOf( conid) );
				stock.put( "smartcontractid", row.getValue("TokenAddress") );
				stock.put( "description", row.getValue("Description") );
				stock.put( "type", row.getValue("Type") );
				stock.put( "exchange", row.getValue("Exchange") );
				m_stocks.add( stock);
				m_stockMap.put( conid, stock);  
			}
		}
	}
	
	String getExchange( int conid) throws RefException {
		return getStock(conid).get("exchange");
	}

	String getSmartContractId( int conid) throws RefException {
		return getStock(conid).get("smartcontractid");
	}
	
	StringJson getStock( int conid) throws RefException {
		StringJson stock = m_stockMap.get( conid);
		require(stock != null, RefCode.NO_SUCH_STOCK, "Error - unknown conid %s", conid);
		return stock;
	}


	/** Manage the connection from this client to TWS. */
	class ConnectionMgr implements IConnectionHandler {
		private String m_host;
		private int m_port;
		private int m_clientId;
		private Timer m_timer;
		private boolean m_ibConnection;
		private final LogType m_logType;
		private final ApiController m_controller = new ApiController( this, null, null);
		boolean ibConnection() { return m_ibConnection; }

		ConnectionMgr(LogType logType) {
			m_logType = logType;
			m_controller.handleExecutions( Main.this);
		}
		
		public ApiController controller() { 
			return m_controller;
		}

		void connect(String host, int port) {
			int clientId = rnd.nextInt( Integer.MAX_VALUE) + 1; // use random client id, but not zero
			S.out( "%s connecting to TWS on %s:%s with client id %s", m_logType, host, port, clientId);
			
			m_host = host;
			m_port = port;
			m_clientId = clientId;
			startTimer();
			
			//S.out( "  done");
		}

		synchronized void startTimer() {
			if (m_timer == null) {
				m_timer = new Timer();
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

		synchronized void onTimer() {
			S.out( "%s trying...", m_logType);
			if (!m_controller.connect(m_host, m_port, m_clientId, "") ) {
				S.out( "%s failed", m_logType);
			}
			else {
				S.out( "%s success", m_logType);
			}
		}

		/** We are connected and have received server version */
		public boolean isConnected() {
			return m_controller.isConnected();
		}
		
		/** Called when we receive server version. We don't always receive nextValidId. */
		@Override public void onConnected() {
			log( m_logType, "Connected to TWS");
			m_ibConnection = true; // we have to assume it's connected since we don't know for sure
			
			stopTimer();
		}
		
		/** Ready to start sending messages. */  // anyone that uses requestid must check for this
		@Override public synchronized void onRecNextValidId(int id) {
			// we really don't care if we get this because we are using random
			// order id's; it's because sometimes, after a reconnect or if TWS
			// is just startup up, or if we tried and failed, we don't ever receive
			// it
			log( m_logType, "Received next valid id %s ***", id);  // why don't we receive this after disconnect/reconnect? pas
		}

		@Override public synchronized void onDisconnected() {
			if (m_timer == null) {
				log( m_logType, "Disconnected from TWS");
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
		
			S.out( "RECEIVED %s %s %s", id, errorCode, errorMsg);
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
	
	class OrderConnectionMgr extends ConnectionMgr {
		OrderConnectionMgr() {
			super( LogType.ORDER_CONNECTION);
		}
	}
	
	/** Handle HTTP msg */
	@Override public synchronized void handle(HttpExchange exch) throws IOException {  // we could/should reduce the amount of synchronization, especially if there are messages that don't require the API
		new MyTransaction( this, exch).handle();
	}

	static String tos(OrderState orderState) {
		return String.format( "state=%s  initMargin=%s  ELV=%s  comm=%s", orderState.status().toString(), orderState.initMarginAfter(), orderState.equityWithLoanAfter(), orderState.commission() );
	}

	public static void require(boolean b, RefCode code, String errMsg, Object... params) throws RefException {
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
		S.out( "-----Dump: Stocks-----");
		MyJsonObject.display( m_stocks, 0, false);
		//S.out( m_stocks);
	}
	
	/** Performs a Redis query wrapped in a pipeline. */
	void jquery(JRun run) { // move into main or MyJedis
		Pipeline p = m_jedis.pipelined();
		run.run(p);
		p.sync();
	}
	
	/** This returns json tags of bid/ask but it might be returning other prices if bid/ask is not available. */
	Prices getPrices(int conid) {
		Map<String, String> ps = m_jedis.hgetAll( String.valueOf(conid) );
		return new Prices( ps);
	}
	
	
}


// get tws running on cloud
// get database running and tested on cloud
// get google access running and tested on cloud

// 
// high: put in a check if an order fills after a timeout; that's a WARNING and ALERT for someone to do something, or for the program to close out the position
// high: add a check for max value; add a test for it 
// you must submit the order at the right price to ensure you get filled at least .4% profit, or whatever

// Bugs
// low: TWS times out overnight, API connects but can't get msgs, at least not contractDetails
// low: on Sunday night, at least, a what-if order returns all Double.max_value strings to api

// shail: what is the ib program not tws i can run for api?
// shail: why does NYSE order go in as smart?
// shail: where is the tws.log file?
// shail can you check IB errorCode 640 for me?
// shail: bug in IB API code cancelTopMktData

// Notes
// All order go in as smart routing
// Paradigm: all threads and runnables should throw and catch RefException

// Later
// you might need throttleing based on IP address to prevent DOS attacks
// lessons: post data is truncated at content length if too short; server hangs waiting for data if too long 
// *probably more efficient to have one timer thread instead of one for each message; fix this when it gets busy
