package redis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimeZone;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;

import common.Util;
import common.Util.ExRunnable;
import http.SimpleTransaction;
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

	private static final SimpleDateFormat hhmmEST = new SimpleDateFormat( "kk:mm:ss");
	private static final MktDataConfig m_config = new MktDataConfig();
	private static final DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses
	static boolean m_debug = false;
	
	private final Stocks m_stocks = new Stocks(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final MdConnectionMgr m_mdConnMgr;
	private final MyRedis m_redis;
	private final TradingHours m_tradingHours; 
	private final ArrayList<DualPrices> m_list = new ArrayList<>();
	private final boolean m_testing = false;  // must be false for production; this is used to put lots of fake prices out
	
	static {
		TimeZone zone = TimeZone.getTimeZone("America/New_York");
		hhmmEST.setTimeZone( zone);			
	}

	public static void main(String[] args) {
		try {
			Util.require( args.length > 0, "Usage: MktDataServer <config_tab>");
			
			// ensure that application is not already running
			SimpleTransaction.listen("0.0.0.0", 6999, SimpleTransaction.nullHandler);			
			
			new MktDataServer(args);
		}
		catch (Exception e) {
			m_log.log( e);
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}


	private MktDataServer(String[] args) throws Exception {
		String tabName = args[0];
		
		// create log file folder and open log file
		log( Util.readResource( Main.class, "version.txt") );  // print build date/time

		if (args.length > 1 && (args[1].equals("/d") || args[1].equals("-d") ) ) {
			m_debug = true;
			log( "debug mode=true");
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
		m_redis = new MyRedis(m_config.redisHost(), m_config.redisPort() );
		m_redis.setName("MktDataServer");
		m_redis.connect();  // test the connection, let it fail now
		S.out( "  done");
		
		m_mdConnMgr = new MdConnectionMgr( m_config.twsMdHost(), m_config.twsMdPort(), m_config.twsMdClientId(), m_config.reconnectInterval() );
		m_tradingHours = new TradingHours(m_mdConnMgr.controller() ); // must come after ConnectionMgr
		
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
		MdConnectionMgr( String host, int port, int clientId, long reconnectInterval) {
			super( host, port, clientId, reconnectInterval);
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

		if (m_config.mode() == Mode.paper) {
			mdController().reqMktDataType(MarketDataType.DELAYED);
		}

		for (Stock stock : m_stocks) {
			final Contract contract = new Contract();
			contract.conid( stock.getConid() );
			
			DualPrices dual = new DualPrices( stock);
			m_list.add( dual);

			// request price on SMART
			if (m_debug) S.out( "  requesting stock prices for %s on SMART", stock.getConid() );
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
				if (m_debug) S.out( "  requesting stock prices for %s on IBESO", stock.getConid() );
				contract.exchange( "IBEOS");
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

	static void log( String text, Object... params) {
		m_log.log( LogType.MDS, text, params);
	}

	static void log( Exception e) {
		m_log.log( e);
	}

	public ApiController mdController() {
		return m_mdConnMgr.controller();
	}

	public ConnectionMgr mdConnMgr() {
		return m_mdConnMgr;
	}

	/** Write to the log file. Don't throw any exception. */

	void wrap( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( JedisConnectionException e) {
			// this happens when writing to redis, e.g. when calling pipeline.hdel( conid, type)
			// we don't know how to recover from this
			
			m_log.log(e);
			System.exit(0);
		}
		catch( Exception e) {
			m_log.log(e);
		}
	}
}
