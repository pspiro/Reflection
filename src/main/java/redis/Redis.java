package redis;

import java.net.BindException;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONArray;

import com.ib.client.Contract;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;

import http.SimpleTransaction;
import json.MyJsonObject;
import json.StringJson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import reflection.Main;
import reflection.Mode;
import reflection.MySqlConnection;
import reflection.Prices;
import reflection.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import util.DateLogFile;
import util.LogType;

public class Redis {
	enum Status { 
		Connected, Disconnected 
	};

	final static Random rnd = new Random( System.currentTimeMillis() );
	final static RedisConfig m_config = new RedisConfig();
	final static MySqlConnection m_database = new MySqlConnection();
	
	final JSONArray m_stocks = new JSONArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final MdConnectionMgr m_mdConnMgr = new MdConnectionMgr();
	private static DateLogFile m_log = new DateLogFile("reflection"); // log file for requests and responses
	private String m_tabName;
	private Jedis m_jedis;
	private Pipeline pipeline;  // access to this is synchronized

	public static void main(String[] args) {
		try {
			String configTab = "Home-config";
			
			if (S.isNull( configTab) ) {
				throw new Exception( "You must specify a config tab name");
			}
			
			// ensure that application is not already running
			SimpleTransaction.listen("0.0.0.0", 6999, SimpleTransaction.nullHandler);			
			
			new Redis().run( configTab);
			
			// you should listen on a port to prevent running dup instances, or some other thing. pas
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
		S.out( "  done");
		
		S.out( "Reading stock list from google sheet");
		readStockListFromSheet();
		S.out( "  done");
		
//		S.out( "Connecting to database %s with user %s", m_config.postgresUrl(), m_config.postgresUser() );
//		m_database.connect( m_config.postgresUrl(), m_config.postgresUser(), m_config.postgresPassword() );
//		S.out( "  done");
		
		S.out( "Connecting to redis server on %s port %s", m_config.redisHost(), m_config.redisPort() );
		m_jedis = new Jedis(m_config.redisHost(), m_config.redisPort() );
		m_jedis.get( "test");
		S.out( "  done");
		
		// connect to TWS
		m_mdConnMgr.connect( m_config.twsMdHost(), m_config.twsMdPort() );
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
				int conid = Integer.valueOf( row.getValue("Conid") );
				String exch = row.getValue("Exchange");
				
				obj.put( "symbol", row.getValue("Symbol") );
				obj.put( "conid", String.valueOf( conid) );
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

			final Contract contract = new Contract();
			contract.conid( Integer.valueOf( conidStr) );
			contract.exchange( exchange);

			
			// you could have the prices flow directly into the Prices Object
			mdController().reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					String type = getTopType( tickType);
					
					// add a timeout for the prices to expire. pas
					// use transactions or pipeline to speed it up. pas
					// use expire() or pexpire()
					
					// we get price=-1 for bid/ask when market is closed
					// we could remove the prices with hdel if we want
					
					if (type != null) {
						if (price > 0) { 
							String val = S.fmt3( price);
							S.out( "ticking %s %s=%s", conidStr, type, val);
							tick( () ->	pipeline.hset( conidStr, type, val) );
						}
						else if (type == "bid" || type == "ask") {
							S.out( "deleting bid/ask");
							tick( () ->	pipeline.hdel( conidStr, type) );
							// delete it!!! pas
						}
					}
				}
			});
		}
	}

	/** Write to the log file. Don't throw any exception. */
	
	static private String getTopType(TickType tickType) {
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
