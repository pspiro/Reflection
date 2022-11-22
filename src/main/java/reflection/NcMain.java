package reflection;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.ITradeReportHandler;
import com.ib.controller.ConnectionAdapter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

/** This, along with NcTransaction is a version that that keeps "no connection"
 *  to server. Somehow, it responds in roughly the same time. This version
 *  would be appropriate to move to google cloud run. */
public class NcMain implements HttpHandler, ITradeReportHandler {
	public enum Mode { 
		paper, production;
	}

	enum Status { 
		Connected, Disconnected 
	};

	final static Random rnd = new Random( System.currentTimeMillis() );
	final static Config m_config = new Config();
	final static MySqlConnection m_database = new MySqlConnection();
	
	final HashMap<Integer,String> m_exchMap = new HashMap<Integer,String>(); // prices could be moved into the Stock object; no need for two separate maps  pas
	final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject

	// we assume that TWS is connected to IB at first but that could be wrong;
	// is there some way to find out?
	private static DateLogFile m_log = new DateLogFile("reflection"); // log file for requests and responses
	private static boolean m_simulated;
	private String m_tabName;
	private String m_host;
	private int m_port;
	
	static boolean simulated() { return m_simulated; }

	JSONArray stocks() { return m_stocks; }
	
	private HttpHandler nullHandler = new HttpHandler() {
		@Override public void handle(HttpExchange exch) throws IOException {
			//S.out( "received null msg " + exch.getHttpContext().getPath() );
		}
	};

	public static void main(String[] args) {
		try {
			new NcMain().run( "Desktop-config");
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
		
		m_host = m_config.twsOrderHost();
		m_port = m_config.twsOrderPort();

		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
		S.out( "  done");

		S.out( "Listening on %s:%s  (%s threads)", m_config.refApiHost(), m_config.refApiPort(), m_config.threads() );
		HttpServer server = HttpServer.create(new InetSocketAddress(m_config.refApiHost(), m_config.refApiPort()+1 ), 0);
		//HttpServer server = HttpServer.create(new InetSocketAddress( m_config.refApiPort() ), 0);
		server.createContext("/favicon", nullHandler); // ignore these requests
		server.createContext("/", this); 
		server.setExecutor( Executors.newFixedThreadPool(m_config.threads()) );  // multiple threads but we are synchronized for single execution
		server.start();
		S.out( "  done");
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
			JSONObject obj = new JSONObject();
			if ("Y".equals( row.getValue( "Active") ) ) {
				int conid = Integer.valueOf( row.getValue("Conid") );
				String exch = row.getValue("Exchange");
				
				obj.put( "symbol", row.getValue("Symbol") );
				obj.put( "conid", String.valueOf( conid) );
				obj.put( "smartcontractid", row.getValue("TokenAddress") );
				obj.put( "description", row.getValue("Description") );
				obj.put( "type", row.getValue("Type") );
				obj.put( "exchange", row.getValue("Exchange") );
				m_stocks.add( obj);
				
				m_exchMap.put( conid, exch);  
			}
		}
	}

	/** Handle HTTP msg */
	@Override public synchronized void handle(HttpExchange exch) throws IOException {  // we could/should reduce the amount of synchronization, especially if there are messages that don't require the API
		new NcTransaction( this, exch).handle();
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

	public String getExchange(int conid) {
		return m_exchMap.get( conid);
	}

	public String tabName() {
		return m_tabName;
	}

	class SingleReq extends ConnectionAdapter {
		private ApiController controller = new ApiController(this);
		private Fun m_func;
		
		void connect(Fun func) {
			m_func = func;
			int clientId = rnd.nextInt( Integer.MAX_VALUE) + 1; // use random client id, but not zero
			controller.connect(m_host, m_port, clientId, "");
		}

		public void onConnected() {
			S.out( "***connected");
			m_func.run(controller);  // you have to add the id
		}
		
		public void onRecNextValidId(int id) {
			S.out( "***rec next valid id " + id);
			//m_func.run(controller);  // you have to add the id
		}
	}
	
	void connect( Fun fun) {
		new SingleReq().connect( fun);
	}
	
	public void reqContractDetails(Contract contract, IContractDetailsHandler handler) {
		connect( controller -> {
			controller.reqContractDetails( contract, list -> {
				controller.disconnect();
				handler.contractDetails( list);
			});
		});
	}
	
	static interface C2 {
		public void run( List<ContractDetails> dets);
	}
	
	static interface Fun {
		public void run( ApiController controller);
	}

	public void cancelOrder(int orderId, String string, Object object) {
		// TODO Auto-generated method stub
		
	}

	public void placeOrModifyOrder(Contract contract, Order order, OrderHandlerAdapter orderHandlerAdapter) {
		// TODO Auto-generated method stub
		
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
// lessons: post data is truncated at content length if too short; server hangs waiting for data if too long