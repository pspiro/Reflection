package http;

import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import fireblocks.Accounts;
import fireblocks.Transactions;
import reflection.Config;
import tw.util.S;

/** Fireblocks polling server */
public class FbServer2 {
	static Accounts accounts = Accounts.instance;
	
	interface Listener {
		void notify(Trans trans);
	}
	
	static HashSet<Listener> m_listeners = new HashSet<>();
	
	static void addListener( Listener listener) {
		m_listeners.add(listener);
	}
	

	// remove COMPLETED items from the queue
	// stop processing when queue is empty
	// start whenever a new order is placed
	
	public static void main(String[] args) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");

		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");
		
		addListener( trans -> S.out(trans) );

		while( true) {
			S.sleep(1000);
			
			// this creates like ten threads for every request which doesn't seem very efficient. pas
			JsonArray ar = Transactions.getSince( System.currentTimeMillis() - 60000 * 3);
			
			for (JsonObject obj : ar) {
				process( new Trans(obj) );
			}
		}
	}
	
	static class Trans {
		JsonObject m_obj;

		Trans(JsonObject obj) {
			m_obj = obj;
		}
		
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
	
//	static class Trans extends JsonObject {
//	}
	
	static HashMap<String,Trans> m_map = new HashMap<>();
	
	static void process(Trans trans) throws Exception {
		//S.out( "%s  %s  hash: %s", fireblocksId, trans.getString("status"), trans.getString("txHash") );
		
		Trans old = m_map.get( trans.id() );
		if (old == null || !trans.status().equals(old.status() ) ) {
			m_map.put(trans.id(), trans);

			for (Listener listener : m_listeners) {
				listener.notify(trans);
			}
		}
	}
}
