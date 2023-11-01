package redis;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;

import org.json.simple.JsonArray;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.sun.net.httpserver.HttpServer;

import common.Util;
import common.Util.ExRunnable;
import redis.clients.jedis.exceptions.JedisConnectionException;
import reflection.Main;
import reflection.Stock;
import reflection.Stocks;
import reflection.TradingHours;
import reflection.TradingHours.Session;
import test.MyTimer;
import tw.google.NewSheet;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

/** Puts bid, ask, last, and last time to redis; pulls bid/ask from either smart or ibeos,
 *  depending on which session we are in. Last always comes from... */
public class MktDataServer {
	//enum Status { Connected, Disconnected };
	enum MyTickType { Bid, Ask, Last };

	public static final String Overnight = "OVERNIGHT"; 
	static boolean m_debug = false;
	static long m_started;  // timestamp that process was started

	
	private final Stocks m_stocks = new Stocks(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	final MdConnectionMgr m_mdConnMgr;
	private final MktDataConfig m_config = new MktDataConfig();
	private final DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses
	private final MyRedis m_redis;
	private final TradingHours m_tradingHours; 
	private final ArrayList<DualPrices> m_list = new ArrayList<>();
	private final boolean m_testing = false;  // must be false for production; this is used to put lots of fake prices out
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("MDS");
			Util.require( args.length > 0, "Usage: MktDataServer <config_tab>");
			new MktDataServer(args);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}


	private MktDataServer(String[] args) throws Exception {
		m_started = System.currentTimeMillis();

		String tabName = args[0];
		
		// create log file folder and open log file
		log( Util.readResource( Main.class, "version.txt") );  // print build date/time

		if (args.length > 1 && (args[1].equals("/d") || args[1].equals("-d") ) ) {
			m_debug = true;
			log( "debug mode=true");
		}
		
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 6999), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/mdserver/status", exch -> new MdTransaction(this, exch).onStatus() ); 
			server.createContext("/mdserver/desubscribe", exch -> new MdTransaction(this, exch).onDesubscribe() ); 
			server.createContext("/mdserver/subscribe", exch -> new MdTransaction(this, exch).onSubscribe() ); 
			server.createContext("/mdserver/disconnect", exch -> new MdTransaction(this, exch).onDisconnect() ); 
			server.createContext("/mdserver/getPrices", exch -> new MdTransaction(this, exch).onGetPrices() ); 
			server.setExecutor( Executors.newFixedThreadPool(10) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
			System.exit(0);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		MyTimer timer = new MyTimer();

		// read config settings from google sheet 
		timer.next("Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		
		timer.next( "Reading stock list from google sheet");
		m_stocks.readFromSheet(m_config);

		// if redis port is zero, host contains the full URI;
		// otherwise, we use host and port
		timer.next("Connecting to redis server on %s:%s", m_config.redisHost(), m_config.redisPort() );
		m_redis = m_config.newRedis();
		m_redis.setName("MktDataServer");
		m_redis.connect();  // test the connection, let it fail now
		S.out( "  done");
		
		m_mdConnMgr = new MdConnectionMgr( this, m_config.twsMdHost(), m_config.twsMdPort(), m_config.twsMdClientId(), m_config.reconnectInterval() );
		m_tradingHours = new TradingHours(m_mdConnMgr.controller(), null); // must come after ConnectionMgr
		
		// connect to TWS
		timer.next("Connecting to TWS");
		m_mdConnMgr.connectNow(); // we want program to terminate if we can't connect to TWS

		// give it 500 ms to get the trading hours; if it's too slow, you'll see a harmless exception
		timer.next( "Start market data update timer");
		Util.executeEvery( 500, m_config.redisBatchTime(), () -> updateRedis(false) ); 
		
		// put out lots of fake prices
		if (m_testing) {
			Util.executeEvery( 1000, 150, () -> {
				if (m_list.size() == 0) return;
				int i = new Random().nextInt(m_list.size() );
				DualPrices dual = m_list.get(i);
				dual.tickSmart( MyTickType.Bid, new Random().nextDouble(100) ); 
				dual.tickSmart( MyTickType.Ask, new Random().nextDouble(100) ); 
				dual.tickSmart( MyTickType.Last, new Random().nextDouble(100) ); 
			});
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> log("Received shutdown msg from linux kill command")));
	}

	/** Refresh list of stocks and re-request market data. */ 
	void refreshStockList() throws Exception {   // never called. pas
		mdController().cancelAllTopMktData();
		m_stocks.readFromSheet(m_config);
		requestPrices();
	}

	class MdConnectionMgr extends ConnectionMgr {
		MdConnectionMgr( MktDataServer main, String host, int port, int clientId, long reconnectInterval) {
			super( main, host, port, clientId, reconnectInterval);
		}
		
		/** Ready to start sending messages. */  // anyone that uses requestid must check for this
		@Override public synchronized void onRecNextValidId(int id) {
			super.onRecNextValidId(id);  // we don't get this after a disconnect/reconnect, so in that case you should use onConnected()
			wrap( () -> requestPrices() );
		}
		
		@Override public void onConnected() {
			super.onConnected();
			m_tradingHours.startQuery();
		}
	}

	/** Might need to sync this with other API calls.  */
	private void requestPrices() throws Exception {
		log( "Requesting prices");
		
		// clear out any existing prices
		m_list.clear();

		if (m_config.mode() == Mode.paper) {
			mdController().reqMktDataType(MarketDataType.DELAYED);
		}

		for (Stock stock : m_stocks) {
			final Contract contract = new Contract();
			contract.conid( stock.getConid() );
			
			DualPrices dual = new DualPrices( stock);
			m_list.add( dual);

			// request price on SMART
			S.out( "  requesting stock prices for %s on SMART", stock.getConid() );
			contract.exchange( "SMART");
			mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					MyTickType myTickType = getTickType(tickType);
					if (myTickType != null) {
						if (m_debug) S.out( "Ticking smart %s %s %s", stock.getConid(), myTickType, price);
						dual.tickSmart(myTickType, price);
					}
				}
			});
			
			// request price on IBEOS
			if (stock.is24Hour() ) {
				S.out( "  requesting stock prices for %s on OVERNIGHT", stock.getConid() );
				contract.exchange( Overnight);
				mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
					@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
						MyTickType myTickType = getTickType(tickType);
						if (myTickType != null) {
							if (m_debug) S.out( "Ticking ibeos %s %s %s", stock.getConid(), myTickType, price);
							dual.tickIbeos(myTickType, price);
						}
					}
				});
			}
		}
	}
	
	/** Check to see if we are in extended trading hours or not so we know which 
	 * market data to use for the ETF's. For now it's hard-coded from 4am to 8pm; 
	 * better would be to check against the trading hours of an actual ETF. 
	 * @throws Exception */
	private void updateRedis(boolean log) {
		m_redis.pipeline( pipeline -> {
			for (DualPrices dual : m_list) {
				try {
					dual.send( pipeline, m_testing 
							? Session.Smart 
							: m_tradingHours.getSession(dual.stock()) );
				}
				catch( Exception e) {
					e.printStackTrace();
				}
			}
		});
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

	public ConnectionMgr mdConnMgr() {
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

	public void desubscribe() {
		mdController().cancelAllTopMktData();
		m_list.clear();
	}

	public void subscribe() {
		wrap( () -> requestPrices() );
	}

	/** Used by Monitor */
	public JsonArray getAllPrices() {
		JsonArray ret = new JsonArray();
		for (DualPrices prices : m_list) {
			prices.addPricesTo( ret);
		}
		return ret;
	}
}
