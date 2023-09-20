package positions;


import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpServer;

import common.Util;
import http.SimpleTransaction;
import reflection.MySqlConnection;
import tw.util.S;

/** This app keeps the positions of all wallets in memory for fast access. */
public class MoralisListener {

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

	public static void main(String[] args) throws Exception {
		new MoralisListener().run( args);
	}
	
	void run(String[] args) throws Exception {
		try {
			// start listening for new events; wait for a response, then query for the missed events
			String host = args[0];
			int port = Integer.valueOf( args[1]);
			startListening(host, port);
		}
		catch( Exception e) {
			e.printStackTrace();  // the program dies here
		}
	}

	private void startListening(String host, int port) throws Exception {
		try {
			S.out( "Listening on %s:%s", host, port);
			HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
			server.createContext("/favicon", exch -> {} ); // ignore these requests
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
		S.out( "----------");
		try {
			trans.getJson().display();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void handleBlockchainEvent2( SimpleTransaction trans) {
		try {
			JsonObject msg = trans.getJson();
			
			JsonObject block = msg.getObject( "block");
			if (block != null) {
				S.out( "-----Block %s", block.getInt("number"));
			}

			S.out( "-----ERC20 transfers");
			for (JsonObject transfer : msg.getArray( "erc20Transfers")) {
	        	String token = transfer.getString( "contract").toLowerCase();
	        	String from = transfer.getString( "from").toLowerCase();
	        	String to = transfer.getString( "to").toLowerCase();
	        	double val = transfer.getDouble( "valueWithDecimals");
	        	String hash = transfer.getString( "transactionHash").toLowerCase();
	        	S.out( "  %s %s %s %s %s", token, from, to, val, hash);  // formats w/ two dec.
	        }
			
			S.out( "");
			S.out( "-----Received logs");
	        for (JsonObject log : msg.getArray( "logs") ) {
	        	String token = log.getString( "address").toLowerCase();
	        	String from = Util.right( log.getString( "topic1"), 42).toLowerCase();
	        	String to = Util.right( log.getString( "topic2"), 42).toLowerCase();
	        	double val = Util.hexToDec( log.getString( "data"), 16);
	        	String hash = log.getString( "transactionHash").toLowerCase();
	        	S.out( "  %s %s %s %s %s", token, from, to, val, hash);  // formats w/ two dec.		        	
	        }
			
			trans.respond( "OK");
		} 
		catch (Exception e) {
			e.printStackTrace();
			trans.respond( "ERROR");  // moralis server does not care what our response is
		}
	}
	
	interface MyFunction {
		void accept(String t) throws Exception;
	}
}
