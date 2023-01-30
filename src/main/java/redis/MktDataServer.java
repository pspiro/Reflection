package redis;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import json.StringJson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import reflection.Main;
import reflection.MySqlConnection;
import reflection.Util;
import tw.google.NewSheet;
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
	private final static MySqlConnection m_database = new MySqlConnection();
	private final static DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses
	//final static int SpyConid = 756733;
	
	private final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final MdConnectionMgr m_mdConnMgr = new MdConnectionMgr();
	private String m_tabName;
	private Jedis m_jedis;
	private Pipeline pipeline;  // access to this is synchronized
	private boolean m_insideHours; // true if we are in the normal ETF trading hours of 4am to 8pm

	static {
		TimeZone zone = TimeZone.getTimeZone("America/New_York");
		hhmm.setTimeZone( zone);			
	}

	public static void main(String[] args) {
		try {
			Util.require( args.length > 0, "Usage: MktDataServer <config_tab>");
			
			// ensure that application is not already running
			SimpleTransaction.listen("0.0.0.0", 6999, SimpleTransaction.nullHandler);			
			
			new MktDataServer().run( args[0] );
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	private void run(String tabName) throws Exception {
		// create log file folder and open log file
		log( LogType.RESTART, Util.readResource( Main.class, "version.txt") );  // print build date/time

		// read config settings from google sheet 
		S.out( "Reading %s tab from google spreadsheet %s", tabName, NewSheet.Reflection);
		m_config.readFromSpreadsheet(tabName);
		S.out( "  done");
		
		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
//		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
//		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
//		S.out( "  done");
		
		S.out( "Connecting to redis server on %s port %s", m_config.redisHost(), m_config.redisPort() );
		//m_jedis = new Jedis(m_config.redisHost(), m_config.redisPort() );
		URI jedisUri = new URI("redis://default:dCPwswvMVvWskdBDAMux201hWEv60Ry0@redis-19488.c1.asia-northeast1-1.gce.cloud.redislabs.com:19488");
		m_jedis = new Jedis(jedisUri);
		m_jedis.get( "test");
		S.out( "  done");
		
		// check every few seconds to see if we are in extended trading hours or not
		// we could check with every tick but that is a lot of expensive time operations
		checkTime(true);
		Util.executeEvery( 5000, () -> checkTime(false) ); 
		
		// connect to TWS
		m_mdConnMgr.connect( m_config.twsMdHost(), m_config.twsMdPort(), m_config.twsMdClientId() );
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> shutdown()));
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
			log( LogType.TIME, "Transitioned trading hours, inside=%s", m_insideHours);
		}
	}

	private void shutdown() {
		S.out("received shutdown msg from linux kill command");
	}

	/** Refresh list of stocks and re-request market data. */ 
	void refreshStockList() throws Exception {   // never called. pas
		mdController().cancelAllTopMktData();
		m_stocks.clear();
		readStockListFromSheet();
		requestPrices();
	}

	// let it fall back to read from a flatfile if this fails. pas
	@SuppressWarnings("unchecked")
	private void readStockListFromSheet() throws Exception {
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, m_config.symbolsTab() ).fetchRows(false) ) {
			StringJson obj = new StringJson();
			if ("Y".equals( row.getValue( "Active") ) ) {
				obj.put( "symbol", row.getValue("Symbol") );
				obj.put( "conid", row.getValue("Conid") );
				obj.put( "smartcontractid", row.getValue("TokenAddress") );
				obj.put( "description", row.getValue("Description") );
				obj.put( "type", row.getValue("Type") );
				obj.put( "exchange", row.getValue("Exchange") );
				m_stocks.add( obj);
			}
		}
	}


	class MdConnectionMgr extends ConnectionMgr {
		MdConnectionMgr() {
			super( LogType.MD_CONNECTION);
		}
		
		/** Ready to start sending messages. */  // anyone that uses requestid must check for this
		@Override public synchronized void onRecNextValidId(int id) {
			super.onRecNextValidId(id);  // we don't get this after a disconnect/reconnect, so in that case you should use onConnected()
			
			try {
				requestPrices();
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	synchronized void tick(Runnable run) {
		if (pipeline == null) {
			pipeline = m_jedis.pipelined();
			Util.executeIn( m_config.batchTime(), () -> syncNow() );
		}
		run.run();
	}
	
	synchronized void syncNow() {
		S.out( "sending to redis");
		pipeline.sync();
		pipeline = null;
	}

	/** Might need to sync this with other API calls.  */
	private void requestPrices() throws Exception {
		S.out( "requesting prices");

		if (m_config.mode() == Mode.paper) {
			mdController().reqMktDataType(MarketDataType.DELAYED);
		}

		for (Object obj : m_stocks) {
			StringJson stock = (StringJson)obj;  
			String conidStr = stock.get("conid");
			String exchange = stock.get("exchange");
			

			// for ETF's request price on SMART and IBEOS
			if (stock.get("type").equals("ETF") ) {
				requestEtfPrice( conidStr);
			}
			else {
				requestStockPrice( conidStr, exchange);
			}
		}
	}
	
	private void requestStockPrice(String conid, String exchange) {
		final Contract contract = new Contract();
		contract.conid( Integer.valueOf( conid) );
		contract.exchange( exchange);

		
		// you could have the prices flow directly into the Prices Object
		mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				tickMktData( conid, tickType, price);
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
					tickMktData(conid, tickType, price);
				}
			}
		});
		
		// request price on IBEOS
		contract.exchange( "IBEOS");
		mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				if (!m_insideHours) {
					tickMktData(conid, tickType, price);
				}
			}
		});
	}
	
	void tickMktData(String conid, TickType tickType, double price) {
		String type = getTickType( tickType);
		
		// add a timeout for the prices to expire. pas
		// use transactions or pipeline to speed it up. pas
		// use expire() or pexpire()
		
		
		if (type != null) {
			if (price > 0) { 
				String val = S.fmt3( price);
				//S.out( "ticking %s %s=%s", conidStr, type, val);
				tick( () ->	pipeline.hset( conid, type, val) );
				
				if (type.equals( "last") ) {
					tick( () ->	pipeline.hset( conid, "time", String.valueOf( System.currentTimeMillis() ) ) );
				}
			}
			// we get price=-1 for bid/ask when market is closed
			else if (type == "bid" || type == "ask") {
				S.out( "clearing %s %s", conid, type);
				tick( () ->	pipeline.hdel( conid, type) );
				// delete it!!! pas
			}
		}
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

	static void log( LogType type, String text, Object... params) {
		m_log.log( type, text, params);
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

}
