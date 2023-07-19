package http;

import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import fireblocks.Accounts;
import fireblocks.Transactions;
import reflection.Config;
import reflection.Util;
import tw.util.S;

/** Fireblocks polling server
 * 
 *  Poll Fireblocks every second to get the status of all orders in the last three minutes.
 *  We could use Webhooks instead, but this server gets the status ten seconds before
 *  the Webhooks server.
 *  
 *  Note that this functionality could be included in the RefAPI but has been broken
 *  out to be able to monitor the resource usage. */
public class FbActiveServer {
	interface Listener {
		void notify(Trans trans);
	}

	static Accounts accounts = Accounts.instance;
	static HashMap<String,Trans> m_map = new HashMap<>();
	static HashSet<Listener> m_listeners = new HashSet<>();
	
	static void addListener( Listener listener) {
		m_listeners.add(listener);
	}
	

	// remove COMPLETED items from the queue
	// stop processing when queue is empty
	// start whenever a new order is placed
	
	public static void main(String[] args) throws Exception {
		S.out( "Starting FbActiveServer");
		accounts.setAdmins( "Admin1,Admin2");

		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");

		// don't allow two instances of the application, and give a way to 
		// test if the application is running
		SimpleTransaction.listen("0.0.0.0", config.fireblocksServerPort(), trans -> trans.respond("OK") );
		
		addListener( trans -> updateRefApi(trans) );

		while( true) {
			S.sleep(2000);
			
			// this creates like ten threads for every request which doesn't seem very efficient. pas
			// we're querying only for transactions in the last three minutes
			JsonArray ar = Transactions.getSince( System.currentTimeMillis() - 60000 * 10);
			
			for (JsonObject obj : ar) {
				process( new Trans(obj) );
			}
		}
	}
	
	static void process(Trans trans) throws Exception {		
		Trans old = m_map.get( trans.id() );
		if (old == null || !trans.status().equals(old.status() ) ) {
			m_map.put(trans.id(), trans);

			S.out( trans.obj() );

			for (Listener listener : m_listeners) {
				listener.notify(trans);
			}
		}
	}
	
	private static void updateRefApi(Trans trans) {
		try {
			String uri = String.format( "/api/fireblocks/?id=%s&status=%s", trans.id(), trans.status() );  

			MyHttpClient client = new MyHttpClient("localhost", 8383);
			client.get(uri);
			
			Util.require( client.getResponseCode() == 200, "Error: received response code " + client.getResponseCode() );
		}
		catch( Exception e) {
			S.out( "Could not update RefAPI - " + e.getMessage() );
		}
	}

	static class Trans {
		JsonObject m_obj;

		Trans(JsonObject obj) {
			m_obj = obj;
		}
		
		JsonObject obj() { return m_obj; }
		
		String id() throws Exception {
			return m_obj.getString("id");  // add the requires)
		}
		
		String hash() throws Exception {
			return m_obj.getString("txHash");
		}
		
		String status() throws Exception {
			return m_obj.getString("status");
		}
		
		@Override public String toString() {
			try {
				return S.format( "%s %s %s", id(), status(), hash() );
			} catch (Exception e) {
				return "error";
			}
		}
		
	}
}
