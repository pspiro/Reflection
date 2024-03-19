package reflection;

import java.util.HashMap;

import org.json.simple.JsonArray;

import common.Util;

/** This subtracts out the quantity of live order so as not to double-spend crypto */
public class UserTokenMgr {
	static class UserToken {
		private double m_offset;

		/** This is called when a new order is placed; it is subtracted out from the current
		 *  balance of the source token to prevent double-spending.
		 *  Increments the m_size
		 *  @return the value of m_size before being incremented */
		public synchronized double increment(double qty) {
			m_offset += qty;
			return m_offset - qty;
		}
		
		public double offset() {
			return m_offset;
		}

		public void decrement(double qty) {
			m_offset -= qty;
		}
	}
	
	static HashMap<String,UserToken> m_map = new HashMap<>();
	
	public static UserToken getUserToken(String wallet, String tokenAddr) {
		String key = wallet.toLowerCase() + tokenAddr.toLowerCase(); 
		return Util.getOrCreate(m_map, key, () -> new UserToken() );
	}

	public static JsonArray getJson() {
		JsonArray ar = new JsonArray();
		m_map.forEach( (tag, userToken) -> ar.add( Util.toJson( 
				"wallet", Util.left( tag, 42),
				"token", Util.right( tag, 42),
				"offset", userToken.offset() ) ) );
		return ar; 
	}
}
