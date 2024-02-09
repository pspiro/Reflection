package positions;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Busd;
import fireblocks.Erc20;
import tw.util.S;

class HookWallet {
	private String m_walletAddr;  // wallet, lower case
	private HashMap<String,Double> m_map = new HashMap<>(); // map contract to token position
	private double m_nativeBal;
	private double m_approved;
	
	HookWallet( String walletAddr, HashMap<String,Double> map, double approved, double nativeBal) {
		m_walletAddr = walletAddr;
		m_map = map;
		m_approved = approved;
		m_nativeBal = nativeBal;
	}
	
	// 1. synchronize access to this map
	// 2. either delete the whole thing or re-query the one that gets cleared
	// at the time because
	/** We get two messages, "unconfirmed" which comes quickly, and "confirmed"
	 *  which comes much later. The position returned by a position query changes
	 *  sometime in between these two messages.
	 *  
	 *  If we get only the second one, it means we missed the first one.
	 *  When we get the second, we could either
	 *  a) ALWAYS query the position (okay), or
	 *  b) query the position ONLY if we didn't see the first one (better)
	 *  
	 *  Supposedly it is possible for confirmed to come first; that will definitely
	 *  break this, and if it happens, we will have to track the order 
	 *  
	 *  @contract must be lower case */
	public void adjust(String contract, double amt, boolean confirmed) throws Exception {
		if (!confirmed) {
			Erc20.inc( m_map, contract, amt);
			S.out( "Updated %s/%s to %s", m_walletAddr, contract, m_map.get(contract) );
		}
		else {
			double bal = new Wallet(m_walletAddr).getBalance( contract);
			
			// this should only occur if we missed the unconfirmed event, i.e. if
			// the HookServer was started after the event came in or if the
			// event came in after the new position was returned in the position query;
			// we should see this infrequently; if we see it frequently, it means
			// I don't understand something
			if (!Util.isEq( bal, m_map.get(contract), HookServer.small) ) {
				S.out( "Warning: updated %s/%s to %s", m_walletAddr, contract, bal);
				m_map.put( contract, bal);
			}
		}
	}

	private JsonArray getJsonPositions() {
		JsonArray ar = new JsonArray();
		m_map.forEach( (address,position) -> {
			if (position >= HookServer.small) {
				ar.add( Util.toJson( "address", address, "position", position) );
			}
		});
		return ar;
	}

	public JsonObject getAllJson() {
		JsonObject obj = new JsonObject();
		obj.put( "native", m_nativeBal);
		obj.put( "approved", m_approved);
		obj.put( "positions", getJsonPositions() );
		return obj;
	}

	public double getBalance(String contract) {
		return Util.toDouble( m_map.get(contract.toLowerCase() ) );
	}

	public double getNativeBalance() {
		return m_nativeBal;
	}

	public double getAllowance(Busd busd, String walletAddr, String rusdAddr) throws Exception {
		return busd.getAllowance(walletAddr, rusdAddr);
	}
}
