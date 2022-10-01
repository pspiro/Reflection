package reflection;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Execution;
import com.ib.client.MarketDataType;
import com.ib.client.OrderState;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.FileUtilities;
import tw.util.OStream;
import tw.util.S;

public class Main implements HttpHandler, ITradeReportHandler {
	public enum Mode { 
		paper, production;
	}

	enum Status { 
		Connected, Disconnected 
	};

	final static Config m_config = new Config();
	final static MySqlConnection m_database = new MySqlConnection();
	
	ApiController m_controller;
	final HashMap<Integer,Prices> m_priceMap = new HashMap<Integer,Prices>(); // prices could be moved into the Stock object; no need for two separate maps  pas
	final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet
	private final ApiHandler m_apiHandler = new ApiHandler(this);
	protected final ConnectionMgr m_connMgr = new ConnectionMgr( m_controller);

	// we assume that TWS is connected to IB at first but that could be wrong;
	// is there some way to find out? pas
	boolean m_ibConnection = true; // is this needed? note that we assume it's connected at first but we don't know for sure
	private static OStream m_log; // log file for requests and responses
	private static boolean m_simulate;
	
	static boolean simulate() { return m_simulate; }

	JSONArray stocks() { return m_stocks; }

	private HttpHandler nullHandler = new HttpHandler() {
		@Override public void handle(HttpExchange exch) throws IOException {
			//S.out( "received null msg " + exch.getHttpContext().getPath() );
		}
	};

	public static void main(String[] args) {
		try {
			String configTab = "Config";
			for (String arg : args) {
				if (arg.equals( "simulated")) {
					m_simulate = true;
					S.out( "Running in simulated mode");
				}
				else {
					configTab = arg;
				}
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

		// read config settings from google sheet; if it fails, fall back to 
		// safe config settings which are known to work
		try {
			S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
			m_config.readFromSpreadsheet(tabName);
			S.out( "  done");
		}
		catch( Exception e) {
			S.out( "ERROR");
			e.printStackTrace();
			m_config.readFromSpreadsheet("Safe Config");
		}

		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
		S.out( "  done");

		S.out( "Listening on %s:%s  (%s threads)", m_config.refApiHost(), m_config.refApiPort(), m_config.threads() );
		HttpServer server = HttpServer.create(new InetSocketAddress(m_config.refApiHost(), m_config.refApiPort() ), 0);
		//HttpServer server = HttpServer.create(new InetSocketAddress( m_config.refApiPort() ), 0);
		server.createContext("/favicon", nullHandler); // ignore these requests
		server.createContext("/", this); 
		server.setExecutor( Executors.newFixedThreadPool(m_config.threads()) );  // multiple threads but we are synchronized for single execution
		server.start();
		S.out( "  done");

		// connect to TWS  // change this to have a connection manager
		m_controller = new ApiController( m_apiHandler, m_apiHandler, m_apiHandler);
		m_controller.handleExecutions( this);
		S.out( "Connecting to TWS on %s:%s with client id %s", m_config.twsHost(), m_config.twsPort(), m_config.apiClientId() );
		m_connMgr.connect( m_config.twsHost(), m_config.twsPort(), m_config.apiClientId() );
	}

	/** Refresh list of stocks and re-request market data. */ 
	void refreshStockList() throws Exception {
		m_controller.cancelAllTopMktData();
		m_stocks.clear();
		m_priceMap.clear();
		readStockListFromSheet();
		requestPrices();
	}

	// let it fall back to read from a flatfile if this fails. pas
	@SuppressWarnings("unchecked")
	private void readStockListFromSheet() throws Exception {
		
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, "Symbols").fetchRows() ) {
			JSONObject obj = new JSONObject();
			if ("Y".equals( row.getValue( "Active") ) ) {
				obj.put( "symbol", row.getValue("Symbol") );
				obj.put( "conid", row.getValue("Conid") );
				obj.put( "smartcontractid", row.getValue("SmartContractID") );
				obj.put( "description", row.getValue("Description") );
				obj.put( "type", row.getValue("Type") );
				obj.put( "exchange", row.getValue("Exchange") );
				m_stocks.add( obj);
			}
		}
	}


	/** Manage the connection from this client to TWS. */
	class ConnectionMgr {
		private String m_host;
		private int m_port;
		private int m_clientId;
		private Timer m_timer;

		ConnectionMgr( ApiController c) {
		}

		public void connect(String host, int port, int clientId) {
			m_host = host;
			m_port = port;
			m_clientId = clientId;
			startTimer();
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
			m_controller.connect(m_host, m_port, m_clientId, "");
		}

		/** We are connected and have received first valid id, so can place orders. */
		public boolean isConnected() {
			return m_controller.isConnected();
		}

		/** Connected and ready to start placing orders. */
		synchronized void onConnected() {
			log( LogType.CONNECTION, "Connected to TWS");
			ibConnection(true); // we have to assume it's connected since we don't know for sure
			stopTimer();
			
			try {
				requestPrices();
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}

		synchronized void onDisconnected() {
			if (m_timer == null) {
				log( LogType.CONNECTION, "Disconnected from TWS");
				m_priceMap.clear();  // clear out all market data since those prices are now stale
				startTimer();
			}
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
	
	private Prices getOrCreatePrices( int conid) {
		Prices prices = m_priceMap.get( conid);
		if (prices == null) {
			prices = new Prices();
			m_priceMap.put( conid, prices);
		}
		return prices;
	}

	/** Might need to sync this with other API calls.   pas */
	private void requestPrices() throws Exception {
		S.out( "requesting prices");

		if (m_config.mode() == Mode.paper) {
			m_controller.reqMktDataType(MarketDataType.DELAYED);
		}

		for (Object obj : m_stocks) {
			JSONObject stock = (JSONObject)obj;  
			int conid = Integer.valueOf( stock.get("conid").toString() );
			String exchange = stock.get("exchange").toString();

			final Contract contract = new Contract();
			contract.conid( conid);
			contract.exchange( exchange);

			final Prices c = getOrCreatePrices( conid);
			
			// simulation mode?
			if (simulate() ) {
				c.setInitialPrices();
				continue;
			}

			m_controller.reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					c.tick(tickType, price, null);
				}
				@Override public void tickSize(TickType tickType, Decimal size) {
					c.tick(tickType, 0.0, size);
				}
			});
		}
	}

	public void ibConnection(boolean connected) {
		S.out( "Broker connection status set to " + connected);
		m_ibConnection = connected;
	}	

	/** Write to the log file. Don't throw any exception. */
	static int date;
	
	static synchronized void log( LogType type, String text, Object... params) {
		try {
			// if date has changed since last log msg, close the log file and create a new one
			if (date != new Date().getDate() ) {
				resetLogFile();
				date = new Date().getDate();
			}
			String str = String.format( "%s %s %s", Util.now(), type, String.format( text, params) );
			S.out( str.substring(13) );
			m_log.writeln( str);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private static void resetLogFile() {
		try {
			String fname = String.format( "logs/reflection.%s.log", Util.today() );
			
			if (m_log != null) {
				m_log.close();
				m_log = null;
				S.out( "Resetting log to %s", fname);
			}

			FileUtilities.createDir( "logs");
			m_log = new OStream( fname);			
		}
		catch( Exception e) {
			e.printStackTrace();
		}
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
		log( LogType.COMMISSION, "%s %s %s %s",
				rpt.execId(), rpt.commission(), rpt.currency(), tradeKey);
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
