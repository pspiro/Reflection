package http;


import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.postgresql.util.PSQLException;

import com.sun.net.httpserver.HttpServer;

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
	static Boolean receivedFirst = false;
	static String nullWallet = "0x0000000000000000000000000000000000000000";
	
	public static void main(String[] args) throws Exception {
		m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");		
		
		String host = args[0];
		int port = Integer.valueOf( args[1]);

		startListening(host, port);
		
		readDatabase();
	}
	
	// should not be called until the database is updated by the other program
	private static void readDatabase() {
		// this must be synced with processing the requests to avoid dups. pas
	}

	private static void startListening(String host, int port) {
		try {
			S.out( "listening on %s:%s", host, port);
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
			server.createContext("/wallet", exch -> handleWalletReq( new SimpleTransaction( exch) ) ); 
			server.createContext("/", exch -> handleJson( new SimpleTransaction( exch) ) ); 
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
	
	static void handleWalletReq( SimpleTransaction trans) {
		try {
			ParamMap map = trans.getMap();
			String wallet = map.get("wallet");
			Main.require( Util.validToken(wallet), RefCode.UNKNOWN, "Invalid wallet");
			S.out( "Handling request for wallet %s", wallet);
			trans.respond( "OK");
		}
		catch( RefException e) {
			trans.respond( e.toJson().toString() );
		}
		catch( Exception e) {
			trans.respond( "Error"); // not good. pas
		}
	}

	static void handleJson( SimpleTransaction trans) {
		try {
			MyJsonObj msg = trans.getJson();
			//msg.displ();
			
			int block = msg.getObj( "block").getInt("number");

			S.out( "ERC20 Transfers");
			for (MyJsonObj transfer : msg.getAr( "erc20Transfers") ) {
	        	String token = transfer.getString( "contract").toLowerCase();
	        	String from = transfer.getString( "from").toLowerCase();
	        	String to = transfer.getString( "to").toLowerCase();
	        	double val = transfer.getDouble( "valueWithDecimals");
	        	String hash = transfer.getString( "transactionHash").toLowerCase();
	        	S.out( "%s %s %s %s %s", token, block, from, to, val);  // formats w/ two dec.

	        	insert( m_database, block, token, from, to, val, hash);
	        }
			
			
			// NOTE: this section can contain log entries other than token transfers, I think
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
			
			// synchronize since this method is potentially called by multiple threads before it returns
//				synchronized( receivedFirst) {
//					if (!receivedFirst) {
//						receivedFirst = true;
//						LogClient.backfill();
//					}
//				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void insert( MySqlConnection db, int block, String token, String from, String to, double val, String hash) throws Exception {
		insert( db, block, token, from, -val, hash);
		insert( db, block, token, to, val, hash);
	}
	
	static void insert( MySqlConnection db, int block, String token, String wallet, double val, String hash) throws Exception {
		try {
			if (val != 0 && !wallet.equals( nullWallet) ) {
				db.execute( String.format( "insert into events values (%s,'%s','%s',%s,'%s')", block, token, wallet, val, hash) );
			}
		}
		catch( PSQLException e) {
			if ( Util.startsWith( e.getMessage(), "ERROR: duplicate key") ) {
				S.out( "Duplicate key inserting into events");
			}
			else throw e;
		}
	}

	
}
