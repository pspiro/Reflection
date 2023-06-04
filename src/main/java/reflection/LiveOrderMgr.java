package reflection;

import java.util.HashMap;

import fireblocks.Transactions;
import json.MyJsonArray;
import json.MyJsonObject;
import tw.util.S;

/** Sends query to FB up to every 1900 ms. When things get busy, we could do this evern n msg
 *  instead of waiting for a query to come in, or, if that's too intensive, change to Webhook model */
public class LiveOrderMgr {
	static final long minInterval = 1900; // make it a little smaller than the Frontend interval from the Working Orders panel

	static long m_lastTime;    // time that we sent last query; wait at least minInterval between queries
	static long m_lastCreated; // lastCreated time of most recent FB transactions
	static HashMap<String,String> m_map = new HashMap<>();  // map FB id -> FB transaction status
	
	static String getStatus(String id) {
		query();
		return m_map.get(id);
	}
	
	private static void query() {
		long elap = System.currentTimeMillis() - m_lastTime;
		if (elap >= minInterval) {
			try {
				MyJsonArray transactions = Transactions.getSince( System.currentTimeMillis() - 3 * 60000); // 3 min
				S.out( "FB transaction returned " + transactions.size() );
				for (MyJsonObject trans : transactions) {
					long createdAt = trans.getLong("createdAt"); 
					String status = trans.getString("status");
					String id = trans.getString("id");
					if (S.isNotNull(status) && S.isNotNull(id) ) {
						m_map.put(id, status); 
					}
					m_lastCreated = Math.max(m_lastCreated, createdAt);
				}
			}
			catch( Exception e) {
				S.out( "Error in LiveOrderMgr thread");
				e.printStackTrace();
			}
			finally {
				m_lastTime = System.currentTimeMillis();
			}
		}
	}

}
