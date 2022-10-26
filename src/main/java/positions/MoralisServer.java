package positions;


import java.net.BindException;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.util.concurrent.Executors;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

import com.sun.net.httpserver.HttpServer;

import http.SimpleTransaction;
import json.MyJsonObj;
import positions.EventFetcher.Balances;
import reflection.Main;
import reflection.MySqlConnection;
import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;
import util.DateLogFile;
import util.LogType;
import util.StringHolder;

/** This app keeps the positions of all wallets in memory for fast access. */
public class MoralisServer {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";

	private static final int high_block = 2000000000;
	static final String chain = "goerli";  // or eth
	static final String farDate = "12-31-2999";
	static final String moralis = "https://deep-index.moralis.io/api/v2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final String transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	
	enum Status { building, waiting, rebuilding, ready, error };

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	static MySqlConnection m_database = new MySqlConnection();

	private Boolean receivedFirst = false;
	private EventFetcher m_client;
	private int m_secondQueryStart;
	private Status m_status = Status.building; // ready to receive position queries
	static DateLogFile m_log = new DateLogFile("position");
	
	// TODO
	// set a single null at the end if you don't want to re-read everything again at startup
	// you can query by date; that would be better, then you only need to know the start date
	// see if you can set up the database to make them all lower case.
	// test that you can handle events while you are sending out the client requests 
	// double-check the synchronization
	// you should periodically query for the current balance and compare to what you have to check for mistakes
	
	public static void main(String[] args) {
		try {
			new MoralisServer().run(args);
		} 
		catch (Exception e) {
			m_log.log( LogType.ERROR, e.getMessage() );				
			e.printStackTrace();
		}
	}
	
	void run(String[] args) throws Exception {
		try {
			S.out( "Connecting to database");
			m_database.connect(dbUrl, dbUser, dbPassword);
			
			// read in the stocks from google sheet			
			m_client = new EventFetcher( this); 
			
			// get current balances for all wallets
			readBalancesFromDb();
			
			int maxDbBlock = readMaxBlockFromDb();
	
			// query highest blockchain block; first query will catch us up to here,
			// second query will get anything that came in afterwards
			int maxChainBlock = queryMaxBlockFromChain();
			
			// read all blocks since the last one, including the last one in case
			// it was partial
			m_client.backfill(maxDbBlock, maxChainBlock);
			
			// we could set a marker that we are caught up at least to maxChainBlock
			// no need to ever re-query data prior to this;
			// keep in mind there might not have been any
			// this doesn't save us much and it pollutes the database
			// m_client.insert( m_database, maxChainBlock, "", "", 0, "", EventFetcher.srcMarker);
			
			// set second query starting point in case any blocks came in while
			// we were backfilling; don't send this query until we know we
			// are receiving live blocks
			m_secondQueryStart = maxChainBlock = 1;
			
			// start listening for new events; wait for a response, then query for the missed events
			String host = args[0];
			int port = Integer.valueOf( args[1]);
			startListening(host, port);
		}
		catch( Exception e) {
			m_status = Status.error;
			m_log.log( LogType.ERROR, e.getMessage() );				
			e.printStackTrace();  // the program dies here
		}
	}

	int readMaxBlockFromDb() throws Exception {
		S.out( "Reading max block from database");
		ResultSet res = m_database.query( "select max(block) from events");
		res.next();
		return res.getInt(1);
	}
	
	/** Read current total for each wallet/token from database into client map */
	private void readBalancesFromDb() throws Exception {
		S.out( "Reading balances from database");
		
		String sql = "select wallet, token, sum(quantity) from events group by wallet, token order by wallet, token";
		ResultSet res = m_database.query( sql);
		while( res.next() ) {
			String wallet = res.getString(1);
			String token = res.getString(2);
			double val = res.getDouble(3);   // look up the symbol
			m_client.increment( wallet, token, val);
		}
	}
	
	int queryMaxBlockFromChain() throws Exception {
		S.out( "Querying max blockchain block");
		
		String url = String.format( "%s/dateToBlock?chain=%s&date=%s",
				moralis, chain, farDate);
		
		String body = MoralisServer.querySync( url);
		MyJsonObj jsonObj = MyJsonObj.parse( body);
		return jsonObj.getInt("block");
	}

	private void startListening(String host, int port) throws Exception {
		m_status = Status.waiting;

		try {
			S.out( "Listening on %s:%s", host, port);
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/wallet", exch -> handleWalletReq( new SimpleTransaction( exch) ) ); 
			server.createContext("/", exch -> handleBlockchainEvent( new SimpleTransaction( exch) ) ); 
			server.setExecutor( Executors.newFixedThreadPool(5) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
			System.exit(0);
		}
	}
	
	void handleBlockchainEvent( SimpleTransaction trans) {
		try {
			MyJsonObj msg = trans.getJson();
			
			int block = msg.getObj( "block").getInt("number");

			S.out( "Received ERC20 transfers");
			for (MyJsonObj transfer : msg.getAr( "erc20Transfers") ) {
	        	String token = transfer.getString( "contract").toLowerCase();
	        	String from = transfer.getString( "from").toLowerCase();
	        	String to = transfer.getString( "to").toLowerCase();
	        	double val = transfer.getDouble( "valueWithDecimals");
	        	String hash = transfer.getString( "transactionHash").toLowerCase();
	        	S.out( "  %s %s %s %s %s", token, block, from, to, val);  // formats w/ two dec.

	        	m_client.insert( m_database, block, token, from, to, val, hash, EventFetcher.srcStream); // i think you can turn off the events to cut down the data. pas
	        }
			
			S.out( "");
			S.out( "Received logs");
	        for (MyJsonObj log : msg.getAr( "logs") ) {
	        	String token = log.getString( "address").toLowerCase();
	        	String from = S.right( log.getString( "topic1"), 42).toLowerCase();
	        	String to = S.right( log.getString( "topic2"), 42).toLowerCase();
	        	double val = Util.hexToDec( log.getString( "data"), 16);
	        	String hash = log.getString( "transactionHash").toLowerCase();
	        	S.out( "  %s %s %s %s %s", block, token, from, to, val);  // formats w/ two dec.		        	
	        }
			
			trans.respond( "OK");
			
			// sync this code because this method gets called by multiple threads 
			synchronized( receivedFirst) {
				if (!receivedFirst) {
					receivedFirst = true;
					
					// don't tie up this handler thread
					Util.execute( () -> {
						try {
							// send second set of backfill queries
							m_status = Status.rebuilding;
							m_client.backfill(m_secondQueryStart, high_block);  // no need to re-read the last block this time because we know it is good

							// we are now all caught up and subscribed so we have 
							// valid data and can process queries
							S.out( "Done backfilling, ready to receive queries");
							m_status = Status.ready;
						}
						catch( Exception e) {
							m_status = Status.error;
						}
					});
				}
			}
			
		} 
		catch (RefException e) {
			m_log.log( LogType.ERROR, e.toString() );				
			trans.respond( "OK");  // this could be responding twice; you should test this or prevent it. pas
		}
		catch (Exception e) {
			m_log.log( LogType.ERROR, e.getMessage() );				
			trans.respond( "OK");  // moralis server does not care what our response is
		}
	}
	
	// remove below code for production. pas
	
	void handleWalletReq( SimpleTransaction trans) {
		try {
			Main.require( m_status == Status.ready || m_status == Status.waiting, RefCode.UNKNOWN, m_status.toString() );

			ParamMap map = trans.getMap();
			String wallet = map.getLowerCase("wallet");

			// request for a single wallet?
			if (S.isNotNull( wallet) ) {
				Main.require( Util.validToken(wallet), RefCode.UNKNOWN, "Invalid wallet");
				m_log.log( LogType.WALLET, "Handling request for wallet %s", wallet);
				
				Balances balances = m_client.getWalletBalances( wallet);
				Main.require( balances != null, RefCode.UNKNOWN, "no balances for wallet");
				trans.respond( balances.toString() );
			}
			else {
				// request for all wallets
				m_log.log( LogType.WALLET, "Handling request for all wallets");
				trans.respond( m_client.getAllWalletsJson() );
			}
		}
		catch( RefException e) {
			m_log.log( LogType.ERROR, e.toString() );
			trans.respondJson( "error", e.toString() ); // toJson().toString() );
		}
		catch( Exception e) {
			m_log.log( LogType.ERROR, e.getMessage() );
			trans.respondJson( "error", e.getMessage() );
		}
	}

	interface MyFunction {
		void accept(String t) throws Exception;
	}

	static String querySync(String url) {
		StringHolder holder = new StringHolder();

	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client.prepare("GET", url)
			.setHeader("accept", "application/json")
			.setHeader("X-API-Key", "test")		
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			holder.val = obj.getResponseBody();
		  		}
		  		catch (Exception e) {
		  			m_log.log( LogType.ERROR, e.getMessage() );
		  			e.printStackTrace();
		  		}
		  	}).join();  // the .join() makes is synchronous

		return holder.val;
	}

}
