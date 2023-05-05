package redis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;

import org.json.simple.JSONArray;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;

import http.SimpleTransaction;
import redis.MyRedis.PRun;
import redis.clients.jedis.exceptions.JedisConnectionException;
import reflection.Main;
import reflection.MyTransaction.ExRunnable;
import reflection.Stock;
import reflection.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

public class MktDataServer {
	enum Status { 
		Connected, Disconnected 
	};

	private final static SimpleDateFormat hhmm = new SimpleDateFormat( "kk:mm:ss");
	private final static Random rnd = new Random( System.currentTimeMillis() );
	        final static MktDataConfig m_config = new MktDataConfig();
	private final static DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses
	//final static int SpyConid = 756733;
	
	private final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private MdConnectionMgr m_mdConnMgr;
	private String m_tabName;
	private MyRedis m_redis;
	private boolean m_insideHours; // true if we are in the normal ETF trading hours of 4am to 8pm
	private boolean m_debug;
	
	static {
		TimeZone zone = TimeZone.getTimeZone("America/New_York");
		hhmm.setTimeZone( zone);			
	}

	public static void main(String[] args) {
		try {
			Util.require( args.length > 0, "Usage: MktDataServer <config_tab>");
			
			// ensure that application is not already running
			SimpleTransaction.listen("0.0.0.0", 6999, SimpleTransaction.nullHandler);			
			
			new MktDataServer().run( args);
		}
		catch (Exception e) {
			m_log.log( e);
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	private void run(String[] args) throws Exception {
		String tabName = args[0];
		
		// create log file folder and open log file
		log( Util.readResource( Main.class, "version.txt") );  // print build date/time

		if (args.length > 1 && (args[1].equals("/d") || args[1].equals("-d") ) ) {
			m_debug = true;
			log( "debug mode=true");
		}

		// read config settings from google sheet 
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		S.out( "  done");
		
		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
		// if redis port is zero, host contains the full URI;
		// otherwise, we use host and port
		S.out( "Connecting to redis server on %s:%s", m_config.redisHost(), m_config.redisPort() );
		m_redis = new MyRedis(m_config.redisHost(), m_config.redisPort() );
		m_redis.setName("MktDataServer");
		m_redis.connect();  // test the connection, let it fail now
		S.out( "  done");
		
		// check every few seconds to see if we are in extended trading hours or not
		// we could check with every tick but that is a lot of expensive time operations
		checkTime(true);
		Util.executeEvery( 5000, () -> checkTime(false) ); 

		// connect to TWS
		m_mdConnMgr = new MdConnectionMgr( m_config.twsMdHost(), m_config.twsMdPort(), m_config.twsMdClientId() );
		m_mdConnMgr.connectNow(); // we want program to terminate if we can't connect to TWS
		//m_mdConnMgr.startTimer();
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> log("Received shutdown msg from linux kill command")));
	}

	/** Check to see if we are in extended trading hours or not so we know which 
	 * market data to use for the ETF's. For now it's hard-coded from 4am to 8pm; 
	 * better would be to check against the trading hours of an actual ETF. */
	private void checkTime(boolean log) {
		boolean inside = m_insideHours;
		
		String now = hhmm.format( new Date() );
		m_insideHours = now.compareTo("03:59:45") >= 0 && now.compareTo("19:59:40") < 0;
		// switch over 20 sec early because we do not want to miss the flood of prices
		// that will come when the new exchange is turned on; note that this will
		// also cause us to miss the -1 that comes at the close of the old exchange
		
		if (log || inside != m_insideHours) {
			log( "Transitioned trading hours, inside=%s", m_insideHours);
		}
	}

	/** Refresh list of stocks and re-request market data. */ 
	void refreshStockList() throws Exception {   // never called. pas
		mdController().cancelAllTopMktData();
		m_stocks.clear();
		readStockListFromSheet();
		requestPrices();
	}

	// let it fall back to read from a flatfile if this fails. pas  you could share this code w/ same method from Main
	@SuppressWarnings("unchecked")
	private void readStockListFromSheet() throws Exception {
		Book book = NewSheet.getBook(NewSheet.Reflection);
		// read master list of symbols and map conid to entry
		HashMap<Integer,ListEntry> map = Main.readMasterSymbols(book);

		for (ListEntry row : book.getTab( m_config.symbolsTab() ).fetchRows(false) ) {
			Stock stock = new Stock();
			if ("Y".equals( row.getString( "Active") ) ) {
				int conid = Integer.valueOf( row.getString("Conid") );

				stock.put( "conid", String.valueOf( conid) );
				
				ListEntry masterRow = map.get(conid);
				Util.require( masterRow != null, "No entry in Master-symbols for conid " + conid);
				stock.put( "symbol", masterRow.getString("Symbol") );
				stock.put( "type", masterRow.getString("Type") ); // Stock, ETF, ETF-24
				stock.put( "exchange", masterRow.getString("Exchange") );

				m_stocks.add( stock);
			}
		}
	}

	class MdConnectionMgr extends ConnectionMgr {
		MdConnectionMgr( String host, int port, int clientId) {
			super( host, port, clientId);
		}
		
		/** Ready to start sending messages. */  // anyone that uses requestid must check for this
		@Override public synchronized void onRecNextValidId(int id) {
			super.onRecNextValidId(id);  // we don't get this after a disconnect/reconnect, so in that case you should use onConnected()
			wrap( () -> requestPrices() );
		}
	}

	/** Create pipeline if necessary and add the tick to the queue 
	 * @throws Exception */
	synchronized void tick(PRun run) throws Exception {
		m_redis.startPipeline( m_config.redisBatchTime(), e -> m_log.log(e) ); // there's a hole here; it's possible that the pipeline was started previous and terminates in between these two calls
		m_redis.runOnPipeline(run);
	}

	/** Might need to sync this with other API calls.  */
	private void requestPrices() throws Exception {
		log( "Requesting prices");

		if (m_config.mode() == Mode.paper) {
			mdController().reqMktDataType(MarketDataType.DELAYED);
		}

		for (Object obj : m_stocks) {
			Stock stock = (Stock)obj;  
			String exchange = "SMART"; //stock.get("exchange");  // exchange would be pulled from the master symbol table
			

			// for ETF's request price on SMART and IBEOS
			// ETF's that trade 24 hours per day have type ETF-24
			if (stock.get("type").equals("ETF-24") ) {
				requestEtfPrice( stock.getConid() );
			}
			else {
				requestStockPrice( stock.getConid(), exchange);
			}
		}
	}
	
	private void requestStockPrice(String conid, String exchange) {
		final Contract contract = new Contract();
		contract.conid( Integer.valueOf( conid) );
		contract.exchange( exchange);

		mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				tickMktData( conid, tickType, price, "stock");
			}
		});
	}

	/** Request prices on both SMART and IBEOS, and then filter based on time */
	private void requestEtfPrice( String conid) {
		final Contract contract = new Contract();
		contract.conid( Integer.valueOf( conid) );

		// request price on SMART
		contract.exchange( "SMART");
		mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				if (m_insideHours) {
					tickMktData(conid, tickType, price, "ETF");
				}
			}
		});
		
		// request price on IBEOS
		contract.exchange( "IBEOS");
		mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				if (!m_insideHours) {
					tickMktData(conid, tickType, price, "IBEOS");
				}
			}
		});
	}
	
	void tickMktData(String conid, TickType tickType, double price, String contractType) {
		wrap( () -> {
			if (m_debug) {
				log( "Ticking %s %s %s %s", contractType, conid, tickType, price);
			}
			
			
			String type = getTickType( tickType);
			
			// add a timeout for the prices to expire. pas
			// use transactions or pipeline to speed it up. pas
			// use expire() or pexpire()
			
			
			if (type != null) {
				if (price > 0) { 
					String val = S.fmt3( price);
					//S.out( "ticking %s %s=%s", conidStr, type, val);
					tick( pipeline -> pipeline.hset( conid, type, val) ); 
					
					if (type.equals( "last") ) {
						tick( pipeline -> pipeline.hset( conid, "time", String.valueOf( System.currentTimeMillis() ) ) );
					}
				}
				// we get price=-1 for bid/ask when market is closed
				else if (type == "bid" || type == "ask") {
					log( "clearing %s %s", conid, type);
					tick( pipeline -> pipeline.hdel( conid, type) );
					// delete it!!! pas
				}
			}
		});
	}

	/** Write to the log file. Don't throw any exception. */
	// better would be to return TickType. pas
	
	static private String getTickType(TickType tickType) {
		String type = null;

		switch( tickType) {
			case CLOSE:
			case DELAYED_CLOSE:
				type = "close"; // not sure what we would ever use this for
				break;
			case LAST:
			case DELAYED_LAST:
				type = "last";
				break;
			case BID:
			case DELAYED_BID:
				type = "bid";
				break;
			case ASK:
			case DELAYED_ASK:
				type = "ask";
				break;
		}
		return type;
	}

	static void log( String text, Object... params) {
		m_log.log( LogType.INFO, text, params);
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

	public String tabName() {
		return m_tabName;
	}

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

//does pipelined() form the connection if not there?
//does query or run on pipe really ever fail?