package redis;

import java.util.ArrayList;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.sun.net.httpserver.HttpExchange;

import common.ConnectionMgrBase;
import common.Util;
import common.Util.ExRunnable;
import http.BaseTransaction;
import http.MyServer;
import redis.DualPrices.Prices;
import redis.clients.jedis.exceptions.JedisConnectionException;
import reflection.Config;
import reflection.Main;
import reflection.Stock;
import reflection.Stocks;
import reflection.TradingHours;
import reflection.TradingHours.Session;
import test.MyTimer;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

/** Puts bid, ask, last, and last time to redis; pulls bid/ask from either smart or ibeos,
 *  depending on which session we are in. Last always comes from... */
public class MdServer {
	//enum Status { Connected, Disconnected };
	enum MyTickType { Bid, Ask, Last, Close, BidSize, AskSize };

	public static final String Overnight = "OVERNIGHT"; 
	public static final String Smart = "SMART"; 
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started
	
	private final Stocks m_stocks = new Stocks(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	        final MdConnectionMgr m_mdConnMgr;
	private final MdConfig m_config = new MdConfig();
	private final DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses
	private final TradingHours m_tradingHours; 
	private final ArrayList<DualPrices> m_list = new ArrayList<>();
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("MDS");
			S.out( "Starting MktDataServer");
			new MdServer(args);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	private MdServer(String[] args) throws Exception {
		// create log file folder and open log file
		log( Util.readResource( Main.class, "version.txt") );  // print build date/time

		if (args.length > 1 && (args[1].equals("/d") || args[1].equals("-d") ) ) {
			BaseTransaction.setDebug(true);
			log( "debug mode=true");
		}
		
		MyTimer timer = new MyTimer();

		// read config settings from google sheet 
		timer.next("Reading configuration");
		m_config.readFromSpreadsheet( Config.getTabName( args) );
		
		timer.next( "Creating http server");
		MyServer.listen( m_config.mdsPort(), 10, server -> {
			server.createContext("/mdserver/status", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/desubscribe", exch -> new MdTransaction( exch).onDesubscribe() ); 
			server.createContext("/mdserver/subscribe", exch -> new MdTransaction( exch).onSubscribe() ); 
			server.createContext("/mdserver/disconnect", exch -> new MdTransaction( exch).onDisconnect() ); 
			server.createContext("/mdserver/refresh", exch -> new MdTransaction( exch).onRefresh() ); 
			server.createContext("/mdserver/get-prices", exch -> new MdTransaction( exch).onGetAllPrices() ); 
			server.createContext("/mdserver/get-ref-prices", exch -> new MdTransaction( exch, false).onGetRefPrices() ); 
			server.createContext("/mdserver/get-stock-price", exch -> new MdTransaction( exch).onGetStockPrice() ); 

			// generic messages
			server.createContext("/mdserver/ok", exch -> new BaseTransaction(exch, false).respondOk() ); // called every few seconds by Monitor
			server.createContext("/mdserver/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/mdserver/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			server.createContext("/", exch -> new BaseTransaction(exch, true).respondNotFound() ); 
		});
		
		timer.next( "Reading stock list from google sheet");
		m_stocks.readFromSheet(m_config.symbolsTab(), null);

		m_mdConnMgr = new MdConnectionMgr( m_config.twsMdHost(), m_config.twsMdPort(), m_config.twsMdClientId(), m_config.reconnectInterval() );
		m_tradingHours = new TradingHours(m_mdConnMgr.controller(), null); // must come after ConnectionMgr
		
		// connect to TWS
		m_mdConnMgr.startTimer();

		Runtime.getRuntime().addShutdownHook(new Thread( () -> onShutdown() ) );
	}

	private void onShutdown() {
		log("Received shutdown msg from linux kill command");
	}

	class MdConnectionMgr extends ConnectionMgrBase {
		MdConnectionMgr( String host, int port, int clientId, long reconnectInterval) {
			super( host, port, clientId, reconnectInterval);
		}
		
		/** Called when we receive server version. We don't always receive nextValidId. */
		@Override public void onConnected() {
			super.onConnected();  // stop the connection time
			log( "Connected to TWS");
			m_tradingHours.startQuery();
		}
		
		/** Ready to start sending messages. */
		@Override public synchronized void onRecNextValidId(int id) {
			S.out( "Received next valid id %s ***", id);
			wrap( () -> requestPrices() );
		}
		
		@Override public synchronized void onDisconnected() {
			if (m_timer == null) {
				log( "Disconnected from TWS");
				startTimer();
			}
		}

		@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
			super.message( id, errorCode, errorMsg, advancedOrderRejectJson);
			
			log( "Received from TWS %s %s %s", id, errorCode, errorMsg);
		}		
	}

	/** Might need to sync this with other API calls.  */
	private void requestPrices() throws Exception {
		log( "Requesting prices");
		
		// clear out any existing prices
		m_list.clear();

		if (m_config.twsDelayed() ) {
			S.out( "Requesting delayed data");
			mdController().reqMktDataType(MarketDataType.DELAYED);
		}

		for (Stock stock : m_stocks) {
			final Contract contract = new Contract();
			contract.conid( stock.conid() );
			
			DualPrices dual = new DualPrices( stock);
			m_list.add( dual);

			// request price on SMART or PAXOS
			contract.exchange( stock.getMdExchange() );
			mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					MyTickType myTickType = getTickType(tickType);
					if (myTickType != null) {
						if (BaseTransaction.debug()) S.out( "Ticking smart %s %s %s", stock.conid(), myTickType, price);
						dual.tickSmart(myTickType, price);
					}
				}
			});
			
			// request price on IBEOS
			if (stock.is24Hour() ) {
				contract.exchange( Overnight);
				mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
					@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
						MyTickType myTickType = getTickType(tickType);
						if (myTickType != null) {
							if (BaseTransaction.debug()) S.out( "Ticking ibeos %s %s %s", stock.conid(), myTickType, price);
							dual.tickIbeos(myTickType, price);
						}
					}
				});
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	static private MyTickType getTickType(TickType tickType) {
		MyTickType type = null;

		switch( tickType) {
			case BID:
			case DELAYED_BID:
				type = MyTickType.Bid;
				break;
			case ASK:
			case DELAYED_ASK:
				type = MyTickType.Ask;
				break;
			case LAST:
			case DELAYED_LAST:
				type = MyTickType.Last;
				break;
			case DELAYED_BID_SIZE:
			case BID_SIZE:
				type = MyTickType.BidSize;
				break;
			case DELAYED_ASK_SIZE:
			case ASK_SIZE:
				type = MyTickType.AskSize;
				break;
			case CLOSE:
			case DELAYED_CLOSE:
				type = MyTickType.Close;
				break;
		}
		return type;
	}

	void log( String text, Object... params) {
		m_log.log( LogType.MDS, text, params);
	}

	void log( Exception e) {
		m_log.log( e);
	}

	public ApiController mdController() {
		return m_mdConnMgr.controller();
	}

	public MdConnectionMgr mdConnMgr() {
		return m_mdConnMgr;
	}

	/** Write to the log file. Don't throw any exception.
	 *  This only wraps one method: requestPrices. */
	void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( JedisConnectionException e) {
			// this happens when writing to redis, e.g. when calling pipeline.hdel( conid, type)
			// we don't know how to recover from this
			
			log(e);
			System.exit(0);
		}
		catch( Exception e) {
			log(e);
		}
	}
	
	/** Change this to a map when we start having more stocks */
	DualPrices getDualPrices(int conid) throws Exception {
		for (DualPrices dual : m_list) {
			if (dual.conid() == conid) {
				return dual;
			}
		}
		throw new Exception( "No dual prices for conid " + conid);
	}

	class MdTransaction extends BaseTransaction {
		MdTransaction(HttpExchange exchange) {
			this( exchange, true);
		}
		
		MdTransaction(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}
		
		public void onStatus() {
			wrap( () -> {
				JsonObject obj = Util.toJson(
						"code", "OK",
						"TWS", m_mdConnMgr.isConnected(),
						"IB", m_mdConnMgr.ibConnection(),
						"mdCount", mdController().mdCount(),
						"started", m_started
						);
				respond( obj);
			});
		}

		/** Refresh list of stocks and re-request market data. */ 
		public void onRefresh() {
			wrap( () -> {
				S.out( "Refreshing list of stock tokens from spreadsheet");
				mdController().cancelAllTopMktData();
				m_stocks.readFromSheet(m_config.symbolsTab(), null);
				requestPrices();
				respondOk();
			});
		}

		public void onSubscribe() {
			wrap( () -> {
				S.out( "Subscribing all");
				requestPrices();
				respondOk();
			});
		}

		public void onDesubscribe() {
			wrap( () -> {
				S.out( "Desubscribing all");
				mdController().cancelAllTopMktData();
				m_list.clear();
				respondOk();
			});
		}

		/** Trigger a disconnect/reconnect to TWS; used to reset MdServer */
		public void onDisconnect() {
			wrap( () -> {
				mdConnMgr().disconnect();
				respondOk();
			});
		}

		/** Used by Monitor. Returns both smart and overnight prices */
		public void onGetAllPrices() {
			wrap( () -> {
				JsonArray ret = new JsonArray();
				for (DualPrices dual : m_list) {
					dual.addPricesTo( ret);
				}
				respond( ret);
			});
		}

		/** Called by Coinstore Server to get bid/ask/last for a single stock */
		public void onGetStockPrice() {
			wrap( () -> {
				DualPrices dual = getDualPrices( getConidFromUri() );

				Prices prices = dual.getPrices( m_tradingHours.getSession( dual.stock() ) );

				double last = prices.last() > 0 
						? prices.last()
						: dual.getAnyLast(); 
				
				respond( Util.toJson( 
						"bid", prices.bid(), 
						"ask", prices.ask(),
						"last", last) ); 
			});
		}

		/** Called by RefAPI. Returns the prices for the current session.
		 * 
		 *  YOU COULD build a static array that doesn't change and just update
		 *  the prices within the array, and always return the same array
		 *  
		 * @throws Exception */
		public void onGetRefPrices() {
			wrap( () -> {
				Session session = null;
				JsonArray ret = new JsonArray();
				
				for (DualPrices dual : m_list) {
					if (session == null) {  // assume the same session for all stocks
						session = m_tradingHours.getSession( dual.stock() );
					}

					JsonObject stockPrices = new JsonObject();
					stockPrices.put( "conid", dual.stock().conid() );
					dual.update(stockPrices, session);
					
					if (m_config.simulateBidAsk() ) {
						double last = stockPrices.getDouble( "last");
						stockPrices.put( "bid", last - .05);
						stockPrices.put( "ask", last + .05);
					}

					ret.add( stockPrices);
				}
				respond( ret);
			});
		}
	}
	
}
