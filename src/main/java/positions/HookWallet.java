package positions;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import refblocks.Refblocks;
import tw.util.S;
import web3.Busd;
import web3.Erc20;
import web3.NodeServer;

class HookWallet {
	private String m_walletAddr;  // wallet, lower case
	private HashMap<String,Double> m_map = new HashMap<>(); // map contract (lower case) to token position
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
	public void adjustERC20(String contract, double amt, boolean confirmed) throws Exception {
		if (!confirmed) {
			Erc20.inc( m_map, contract, amt);
			S.out( "Updated %s / %s to %s", m_walletAddr, contract, m_map.get(contract) );
		}
		else {
			double bal = NodeServer.getBalance( contract, m_walletAddr, 0);
			if (!Util.isEq( bal, m_map.get(contract), HookServer.small) ) {
				// this should only occur if we missed the unconfirmed event, i.e. if
				// the HookServer was started after the event came in or if the
				// event came in after the new position was returned in the position query;
				// we should see this infrequently; if we see it frequently, it means
				// I don't understand something
				S.out( "Warning: updated %s / %s to %s", m_walletAddr, contract, bal);
				m_map.put( contract, bal);
			}
		}
	}
	
	public void adjustNative( double amt, boolean confirmed) throws Exception {
		if (!confirmed) {
			m_nativeBal += amt;
			S.out( "Updated native balance in %s to %s", m_walletAddr, m_nativeBal);
		}
		else {
			double bal = NodeServer.getNativeBalance(m_walletAddr);
			if (!Util.isEq( bal, m_nativeBal, HookServer.small) ) {
				S.out( "Warning: updated native balance in %s to %s", m_walletAddr, bal);
				m_nativeBal = bal;
			}
		}
	}

	/** Return all positions; used for My Reflection panel */
	private JsonArray getJsonPositions(double min) {
		JsonArray ar = new JsonArray();
		m_map.forEach( (address,position) -> {
			if (position >= min) {
				ar.add( Util.toJson( "address", address, "position", position) );
			}
		});
		return ar;
	}

	private JsonObject getJsonPositionsMap(double min) {
		JsonObject map = new JsonObject();
		m_map.forEach( (address,position) -> {
			if (position >= min) {
				map.put( address, position);
			}
		});
		return map;
	}

	/** For debugging */
	public JsonObject getAllJson(double min) {
		JsonObject obj = new JsonObject();
		obj.put( "native", m_nativeBal);
		obj.put( "approved", m_approved);
		obj.put( "positions", getJsonPositions(min) );
		obj.put( "wallet", m_walletAddr);
		return obj;
	}

	/** Used by RefAPI to check balances before an order */ 
	public JsonObject getAllJsonMap(double min) {
		JsonObject obj = new JsonObject();
		obj.put( "native", m_nativeBal);
		obj.put( "approved", m_approved);
		obj.put( "positions", getJsonPositionsMap(min) );
		obj.put( "wallet", m_walletAddr);
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

	public void approved(double amt) {
		m_approved = amt;
	}

}
