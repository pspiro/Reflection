package positions;


import java.net.BindException;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import http.MyJsonObj;
import http.SimpleTransaction;
import positions.EventFetcher.Balances;
import reflection.Main;
import reflection.MySqlConnection;
import reflection.ParamMap;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class MoralisServer {

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	static MySqlConnection m_database = new MySqlConnection();

	private Boolean receivedFirst = false;
	private EventFetcher m_client;
	private int m_maxBlock;
	
	// TODO
	// you must query the latest block on the chain and use that as the ending block and
	// then set a single null entry in the db so you don't requery everything again if it's restarted
	// 1. get the last block
	// 2. subscribe; you have to make sure your subscription is getting blocks before you query missing blocks; maybe wait a little while
	// 3. query the missing blocks
	// add transaction has to db and make a unique key on it
	// * test it with a very small page size
	// * fill in the blanks so it doesn't send queries from blocks w/ no entries every time
	// query the database
	
	// this query is very slow; why is that?
	// you can query by date; that would be better, then you only need to know the start date
	// see if you can set up the database to make them all lower case. pas
	// test that you can handle events while you are sending out the client requests 
	// double-check the synchronization
	// add more comments
	
	public static void main(String[] args) {
		try {
			new MoralisServer().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void run(String[] args) throws Exception {
		S.out( "Connecting to database");
		m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
		
		// get current highest block; must be done before we start listening for new events
		S.out( "Querying database for highest block");
		ResultSet res = MoralisServer.m_database.query( "select max(block) from events");
		res.next();
		m_maxBlock = res.getInt(1);
		S.out( "  max block is %s", m_maxBlock);
		
		m_client = new EventFetcher( this);
		
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		
		// start listening for new events; wait for a response, they query for the missed events
		startListening(host, port);
		
		m_client.backfill(m_maxBlock);  // FOR TESTING ONLY, REMOVE THIS. pas
		
	}
	
	private void startListening(String host, int port) {
		try {
			S.out( "listening on %s:%s", host, port);
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/wallet", exch -> handleWalletReq( new SimpleTransaction( exch) ) ); 
			server.createContext("/", exch -> handleBlockchainEvent( new SimpleTransaction( exch) ) ); 
			server.setExecutor( Executors.newFixedThreadPool(5) );
			server.start();
		}
		catch( BindException e) {
			S.out( "The application is already running");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void handleBlockchainEvent( SimpleTransaction trans) {
		try {
			MyJsonObj msg = trans.getJson();
			
			int block = msg.getObj( "block").getInt("number");

			S.out( "ERC20 Transfers");
			for (MyJsonObj transfer : msg.getAr( "erc20Transfers") ) {
	        	String token = transfer.getString( "contract").toLowerCase();
	        	String from = transfer.getString( "from").toLowerCase();
	        	String to = transfer.getString( "to").toLowerCase();
	        	double val = transfer.getDouble( "valueWithDecimals");
	        	String hash = transfer.getString( "transactionHash").toLowerCase();
	        	S.out( "%s %s %s %s %s", token, block, from, to, val);  // formats w/ two dec.

	        	m_client.insert( m_database, block, token, from, to, val, hash); // i think you can turn off the events to cut down the data. pas
	        }
			
			S.out( "");
			S.out( "Logs");
	        for (MyJsonObj log : msg.getAr( "logs") ) {
	        	String token = log.getString( "address").toLowerCase();
	        	String from = S.right( log.getString( "topic1"), 42).toLowerCase();
	        	String to = S.right( log.getString( "topic2"), 42).toLowerCase();
	        	double val = Util.hexToDec( log.getString( "data"), 16);
	        	String hash = log.getString( "transactionHash").toLowerCase();
	        	S.out( "%s %s %s %s %s", block, token, from, to, val);  // formats w/ two dec.		        	
	        }
			
			trans.respond( "OK");
			
			
			// sync this code because this method gets called by multiple threads 
			synchronized( receivedFirst) {
				if (!receivedFirst) {
					receivedFirst = true;
					
					// don't tie up this handler thread
					Util.execute( () -> {
						try {
							m_client.backfill(m_maxBlock);
						}
						catch( Exception e) {
							e.printStackTrace();
						}
					});
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void handleWalletReq( SimpleTransaction trans) {
		try {
			ParamMap map = trans.getMap();
			String wallet = map.getLowerCase("wallet");

			// request for a single wallet?
			if (S.isNotNull( wallet) ) {
				Main.require( Util.validToken(wallet), RefCode.UNKNOWN, "Invalid wallet");
				S.out( "Handling request for wallet %s", wallet);
				Balances balances = m_client.getWalletBalances( wallet);
				Main.require( balances != null, RefCode.UNKNOWN, "no balances for wallet");
				trans.respond( balances.toString() );
			}
			else {
				// request for all wallets
				trans.respond( m_client.getAllWalletsJson() );
			}
		}
		catch( RefException e) {
			trans.respond( e.toJson().toString() );
		}
		catch( Exception e) {
			trans.respond( "Error"); // not good. pas
		}
	}
}
