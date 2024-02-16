package fireblocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.BaseTransaction;
import http.MyClient;
import http.MyHttpClient;
import reflection.Config;
import reflection.FireblocksStatus;
import tw.util.S;
import static common.Util.hhmmss;

/** Fireblocks polling server
 * 
 *  Poll Fireblocks every second to get the status of all orders in the last three minutes.
 *  We could use Webhooks instead, but this server gets the status ten seconds before
 *  the Webhooks server.
 *  
 *  Note that transactions are never deleted; if the map gets large, you'll need to fix this. 
 *  
 *  Note that this functionality could be included in the RefAPI but has been broken
 *  out to be able to monitor the resource usage. Adding it to RefAPI would be a simpler solution. */
public class FbServer {
	static final TreeMap<String,Trans> m_map = new TreeMap<>(); /** Map fireblock id to transaction */
	static final Config m_config = new Config();
	static long m_started;
	static long m_lastSuccessfulFetch;
	static long m_lastSuccessfulPut;
	static long m_lastQueryTime;
	
	// remove COMPLETED items from the queue
	// stop processing when queue is empty
	// start whenever a new order is placed
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("FBAS");
			S.out( "Starting FbServer");
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
		MyClient.filename = "fbserver.http.log";

		m_config.readFromSpreadsheet(tab);
		
		Runtime.getRuntime().addShutdownHook(new Thread( () -> S.out("Shutdown message received") ) );
		
		MyServer.listen( m_config.fbServerPort(), 10, server -> {
			server.createContext("/fbserver/status", exch -> new FbTransaction(exch).onStatus() );
			server.createContext("/fbserver/get-all", exch -> new FbTransaction(exch).onGetAll() );
			
			server.createContext("/fbserver/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/fbserver/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/fbserver/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
		});
		
		while( true) {
			S.sleep( m_config.fbPollIingInterval() );
			
			queryFireblocks();
			
			updateRefApi();
			
			prune();  // only delete sent final items at the beginning of the map up to the first on that is not
		}
	}
	
	private static void queryFireblocks() {
		try {			
			long now = System.currentTimeMillis();

			// start with the earliest unfinished transaction
			long start = Long.MAX_VALUE;
			for (Trans trans : m_map.values() ) {
				if (!trans.isFinal() ) {
					start = Math.min(start, trans.createdAt() );
				}
			}
			
			// if there are none, start from the last successful request minus 10 secs
			if (start == Long.MAX_VALUE) {
				start = m_lastSuccessfulFetch == 0
						? System.currentTimeMillis() - (long)(60000 * m_config.fbLookback())  // used at startup
						: m_lastSuccessfulFetch - 10000; // go back extra 10 sec in case our clocks are off; there's no harm if we re-fetch old transactions that were already sent
			}				

			// fetch transactions
			JsonArray ar = Transactions.getSince( start);
			S.out( "Queried back %s seconds from %s", (now - start) / 1000, hhmmss.format(start) );
			m_lastSuccessfulFetch = now;

			// debug
			if (BaseTransaction.debug()) {
				S.out();
				S.out( "Transactions");
				S.out(ar);
				S.out();
			}

			// update map if status has changed; this will clear the m_sent flag
			for (JsonObject obj : ar) {
				Trans trans = new Trans(obj);

				Trans old = m_map.get( trans.id() );
				if (old == null || !trans.status().equals(old.status() ) ) {
					m_map.put(trans.id(), trans);
					S.out( "Updated %s  %s  %s", trans.id(), trans.status(), hhmmss.format(trans.createdAt() ) );
				}
			}
		}
		catch( Exception e) {
			S.out( "Error fetching transactions - " + e);
		}
	}
	
	/** Loop through the map; send any changed entries to RefAPI,
	 *  then delete any final entries */
	private static void updateRefApi() {
		try {
			for (Iterator<String> iter = m_map.keySet().iterator(); iter.hasNext(); ) {
				
				String id = iter.next();
				Trans trans = m_map.get(id);
				
				Util.require( trans != null, "Impossible!");

				if (!trans.sent() ) {
					
					// update RefAPI with new or changed status
					MyHttpClient client = new MyHttpClient("localhost", m_config.refApiPort() );
					client.get( String.format( "/api/fireblocks/?id=%s&status=%s%s",	
							trans.id(),
							trans.status(),
							S.isNotNull(trans.hash()) ? "&txhash=" + trans.hash() : "") // append hash only if not null; RefAPI can't handle null params in URI
							);  	
					
					Util.require( 
							client.getResponseCode() == 200, 
							"Error: response code: %s  message: %s", client.getResponseCode(), client.getMessage() );
					
					S.out( "Sent %s %s to RefAPI", id, trans.status() );
					trans.sent( true); // flag it as sent; we won't resend until the status changes
				}
			}
			
			m_lastSuccessfulPut = System.currentTimeMillis();
		}
		catch( Exception e) {
			S.err( "Error putting transactions", e);
		}
	}

	/** Remove transactions at the beginning of the list that are final and sent to RefAPI
	 *  up to the first one that is not */
	private static void prune() {
		// sort the transactions by "createdAt"
		ArrayList<Trans> recs = new ArrayList<>(m_map.values());
		recs.sort( (o1, o2) -> comp( o1.createdAt(), o2.createdAt() ) );

		for (Trans trans : recs) {
			if (trans.isFinal() && trans.sent() ) {
				m_map.remove( trans.id() );
				S.out( "Pruned %s %s new size %s", trans.id(), trans.status(), m_map.size() );
			}
			else {
				break;
			}
		}
	}

	static int comp(long v1, long v2) {
		return v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
	}

	/** This is a Fireblocks json transaction */
	static class Trans {
		private JsonObject m_obj;
		private boolean m_sent;
		private long m_createdAt;  // used for sorting

		Trans(JsonObject obj) {
			m_obj = obj;
			m_createdAt = obj.getLong("createdAt");
		}
		
		void sent(boolean v) {
			m_sent = v;
		}
		
		boolean sent() {
			return m_sent;
		}
		
		public boolean isFinal() {
			try {
				return Util.getEnum(status(), FireblocksStatus.values() ).pct() == 100; 
			}
			catch( Exception e) {
				S.err( "Error", e);
				return false;
			}
		}

		JsonObject obj() { return m_obj; }
		
		String id() {
			return m_obj.getString("id");  // fireblocks id
		}
		
		String hash() {
			return m_obj.getString("txHash");
		}
		
		String status() {
			return m_obj.getString("status");
		}
		
		long createdAt() {
			return m_createdAt;
		}
		
		long lastUpdated() {
			return m_obj.getLong("lastUpdated");
		}
		
		@Override public String toString() {
			try {
				return m_obj.toString();
				//return S.format( "%s %s %s %s", m_obj.getString("note"), id(), status(), hash() );
			} catch (Exception e) {
				return "error";
			}
		}
		
	}
}
