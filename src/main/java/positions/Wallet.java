package positions;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.Erc20;
import web3.MoralisServer;
import web3.NodeServer;

/** Get token positions; will only send one query
 * @deprecated  */
public class Wallet {
	private String m_walletAddr;
	
	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_walletAddr = address;
	}
	
	public String walletAddr() {
		return m_walletAddr;
	}

	/** Returns a map of contract address (lower case) -> position (Double).
	 *  This version retrieves the map from Moralis is is having issues of
	 *  missing positions as of 1/23/24 
	 *  I think passing the contracts may fix it.
	 *  They are claiming it is fixed as of 1/26/24 */ 
	public HashMap<String,Double> reqPositionsMap(String... contracts) throws Exception {
		//Util.require( contracts.length > 0, "Contract addresses are required");  // needed to to Moralis bug
		
		HashMap<String,Double> map = new HashMap<>();
		
		for (JsonObject token : MoralisServer.reqPositionsList(m_walletAddr, contracts) ) {
			String addr = token.getString("token_address");			
			String balance = token.getString("balance");
			
			if (S.isNotNull(addr) && S.isNotNull(balance) ) {
				int decimals = token.getInt("decimals");
				
				// this was a bug that they fixed so should not happen anymore
				// (it still seems to happen with spam tokens as of 7/30/24)
				if (decimals == 0) {
					S.out( "Error: Moralis query failed to return number of decimals for %s; defaulting to 18", addr);
					decimals = 18;
				}
				
				map.put( addr.toLowerCase(), Erc20.fromBlockchain(balance, decimals) );
			}
		}
		
		return map;
	}
	
}
