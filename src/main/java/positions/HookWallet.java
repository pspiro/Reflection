package positions;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import tw.util.S;

class HookWallet {
	private String m_walletAddr;
	private HashMap<String,Double> m_map = new HashMap<>(); // map contract to token position
	private double m_nativeTok;
	private double m_approved;
	
	HookWallet( String walletAddr, HashMap<String,Double> map) {
		m_walletAddr = walletAddr;
		m_map = map;
	}
	
	// 1. synchronize access to this map
	// 2. either delete the whole thing or re-query the one that gets cleared
	// at the time because 
	public void adjust(String contract, double amt, boolean confirmed) {
		if (!confirmed) {
			Erc20.inc( m_map, contract, amt);
			S.out( "Updated %s/%s to %s", m_walletAddr, contract, m_map.get(contract) );
		}
		else {
			S.out( "Cleared out %s/%s", m_walletAddr, contract);
			m_map.remove( contract);
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
		obj.put( "native", m_nativeTok);
		obj.put( "approved", m_approved);
		obj.put( "positions", getJsonPositions() );
		return obj;
	}
}
