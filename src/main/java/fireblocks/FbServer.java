package fireblocks;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import reflection.Config;
import tw.util.S;

/** Fireblocks polling server
 * 
 *  Poll Fireblocks every second to get the status of all orders in the last three minutes.
 *  We could use Webhooks instead, but this server gets the status ten seconds before
 *  the Webhooks server.
 *  
 *  Note that this functionality could be included in the RefAPI but has been broken
 *  out to be able to monitor the resource usage. Adding it to RefAPI would be a simpler solution. */
public class FbServer {
	static HashMap<String,Trans> m_map = new HashMap<>();
	static long m_started;
	static boolean m_debug;
	static Config m_config = new Config();
	
	// remove COMPLETED items from the queue
	// stop processing when queue is empty
	// start whenever a new order is placed
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("FBAS");
			S.out( "Starting FbActiveServer");
			Util.require( args.length >= 1, "You must specify a config tab name");
			run( args[0] );
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}
	
	public static void run(String tab) throws Exception {
		m_started = System.currentTimeMillis();

		m_config.readFromSpreadsheet(tab);
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> S.out("Shutdown message received") ) );
		
		MyServer.listen( m_config.fbServerPort(), 10, server -> {
			server.createContext("/fbserver/ok", exch -> new FbTransaction(exch).onOk() ); 
			server.createContext("/fbserver/status", exch -> new FbTransaction(exch).onStatus() );
			server.createContext("/fbserver/debug-on", exch -> new FbTransaction(exch).onDebug(true) );
			server.createContext("/fbserver/debug-off", exch -> new FbTransaction(exch).onDebug(false) );
		});
		
		while( true) {
			S.sleep( m_config.fbPollIingInterval() );
			
			// we're querying only for transactions in the last three minutes
			// (Q: is this ones started in last three or updated in last three?)
			try {
				JsonArray ar = Transactions.getSince( System.currentTimeMillis() - (long)(60000 * m_config.fbLookback()) );
				
				if (m_debug) {
					S.out();
					S.out( "Transactions");
					S.out(ar);
					S.out();
				}
				
				for (JsonObject obj : ar) {
					process( new Trans(obj) );
				}
			}
			catch( Exception e) {
				S.out( "Error - " + e.getMessage() );
				e.printStackTrace();
			}
		}
	}
	
	static void process(Trans trans) throws Exception {		
		Trans old = m_map.get( trans.id() );
		if (old == null || !trans.status().equals(old.status() ) ) {
			m_map.put(trans.id(), trans);

			S.out( m_debug ? trans.obj() : trans);  // in debug mode, print the whole object
			
			try {
				String uri = String.format( "/api/fireblocks/?id=%s&status=%s", 
						trans.id(), trans.status() );
				
				// append hash only if not null; RefAPI can't handle null params in URI
				if (S.isNotNull(trans.hash() ) ) {
					uri = String.format("%s&txHash=%s", uri, trans.hash() );
				}
	
				// use MyHttpClient since this is a local transaction
				MyHttpClient client = new MyHttpClient("localhost", m_config.refApiPort() );
				client.get(uri);
				
				Util.require( 
						client.getResponseCode() == 200, 
						"Error: response code: %s  message: %s", client.getResponseCode(), client.getMessage() );
			}
			catch( Exception e) {
				S.out( "Could not update RefAPI - " + e.getMessage() );
			}
		}
	}

	/** This is a Fireblocks json transaction */
	static class Trans {
		JsonObject m_obj;

		Trans(JsonObject obj) {
			m_obj = obj;
		}
		
		JsonObject obj() { return m_obj; }
		
		String id() throws Exception {
			return m_obj.getString("id");  // fireblocks id
		}
		
		String hash() throws Exception {
			return m_obj.getString("txHash");
		}
		
		String status() throws Exception {
			return m_obj.getString("status");
		}
		
		@Override public String toString() {
			try {
				return S.format( "%s %s %s %s", m_obj.getString("note"), id(), status(), hash() );
			} catch (Exception e) {
				return "error";
			}
		}
		
	}
}
