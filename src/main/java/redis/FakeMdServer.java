package redis;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyServer;
import redis.DualPrices.Prices;
import reflection.Config;
import reflection.Main;
import reflection.Stock;
import reflection.Stocks;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

/** Puts bid, ask, last, and last time to redis; pulls bid/ask from either smart or ibeos,
 *  depending on which session we are in. Last always comes from... */
public class FakeMdServer {
	//enum Status { Connected, Disconnected };
	enum MyTickType { Bid, Ask, Last, Close, BidSize, AskSize };

	public static final String Overnight = "OVERNIGHT"; 
	public static final String Smart = "SMART"; 
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started
	
	private final Stocks m_stocks = new Stocks(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final MdConfig m_config = new MdConfig();
	private final DateLogFile m_log = new DateLogFile("mktdata"); // log file for requests and responses

	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("MDS");
			S.out( "Starting MktDataServer");
			new FakeMdServer(args);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	private FakeMdServer(String[] args) throws Exception {
		// create log file folder and open log file
		log( Util.readResource( Main.class, "version.txt") );  // print build date/time

		if (args.length > 1 && (args[1].equals("/d") || args[1].equals("-d") ) ) {
			BaseTransaction.setDebug(true);
			log( "debug mode=true");
		}
		
		
		m_stocks.fakeInit();

		m_config.readFromSpreadsheet( Config.getTabName( args) );
		
		MyServer.listen( m_config.mdsPort(), 10, server -> {
			server.createContext("/mdserver/status", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/desubscribe", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/subscribe", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/disconnect", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/refresh", exch -> new MdTransaction( exch).onStatus() ); 
			server.createContext("/mdserver/get-prices", exch -> new MdTransaction( exch).onGetRefPrices() ); 
			server.createContext("/mdserver/get-ref-prices", exch -> new MdTransaction( exch, false).onGetRefPrices() ); 

			// generic messages
			server.createContext("/mdserver/ok", exch -> new BaseTransaction(exch, false).respondOk() ); // called every few seconds by Monitor
			server.createContext("/", exch -> new BaseTransaction(exch, true).respondNotFound() ); 
		});
		
		m_stocks.readFromSheet(m_config);
		m_stocks.fakeInit();
	}


	void log( String text, Object... params) {
		m_log.log( LogType.MDS, text, params);
	}

	void log( Exception e) {
		m_log.log( e);
	}

	class MdTransaction extends BaseTransaction {
		MdTransaction(HttpExchange exchange) {
			this( exchange, true);
		}
		
		MdTransaction(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}
		
		public void onStatus() {
			wrap( () -> respondOk() );
		}

		/** Called by RefAPI. Returns the prices for the current session.
		 * 
		 *  YOU COULD build a static array that doesn't change and just update
		 *  the prices within the array, and always return the same array
		 *  
		 * @throws Exception */
		
		
		public void onGetRefPrices() {
			wrap( () -> {
				JsonArray ret = new JsonArray();
				
				for (Stock stock : m_stocks) {
					JsonObject stockPrices = stock.prices().toJson( 0);
					stockPrices.put( "last", stock.prices().last() );
					stockPrices.put( "conid", stock.conid() );
					ret.add( stockPrices);
				}

				respond( ret);
			});
		}
	}
	
}
