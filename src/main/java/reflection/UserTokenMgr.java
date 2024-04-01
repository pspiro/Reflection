package reflection;

import java.util.HashMap;

import org.json.simple.JsonArray;

import common.Util;

/** This subtracts out the quantity of live orders so as not to double-spend crypto.
 *  The orders are removed when they fail or when they become CONFIRMING as per
 *  Fireblocks. There will be a small interval between the time we get the CONFIRMING 
 *  and the contract balance changes as per HookServer where we will have a wrong
 *  balance; it's fine. */
public class UserTokenMgr {
	static class UserToken {
		private double m_offset;
		private long m_createdAt;
		private long m_updatedAt;
		
		public double offset() { return m_offset; }

		UserToken() {
			m_createdAt = System.currentTimeMillis();
			m_updatedAt = m_createdAt;
		}

		/** This is called when a new order is placed; it is subtracted out from the current
		 *  balance of the source token to prevent double-spending.
		 *  Increments the m_size
		 *  @return the value of m_size BEFORE being incremented */
		public synchronized double increment(double qty) {
			m_updatedAt = System.currentTimeMillis();
			m_offset += qty;
			return m_offset - qty;
		}

		public void decrement(double qty) {
			m_updatedAt = System.currentTimeMillis();
			m_offset -= qty;
		}
	}
	
	/** Map walletAddr+tokenAddr to UserToken
	 *  Don't serialize this; if RefAPI is restarted, worst case is user tries
	 *  to double-spend and the blockchain transaction fails. If we restore wrong
	 *  data from the file, it will stay wrong forever. */
	private static HashMap<String,UserToken> m_map = new HashMap<>();
	
	public static UserToken getUserToken(String wallet, String tokenAddr) {
		String key = wallet.toLowerCase() + tokenAddr.toLowerCase(); 
		return Util.getOrCreate(m_map, key, () -> new UserToken() );
	}

	public static JsonArray getJson() {
		JsonArray ar = new JsonArray();
		m_map.forEach( (tag, userToken) -> ar.add( Util.toJson(
				"createdAt", userToken.m_createdAt,
				"updatedAt", userToken.m_updatedAt,
				"wallet", Util.left( tag, 42),
				"token", Util.right( tag, 42),
				"offset", userToken.offset() ) ) );
		return ar; 
	}
}
