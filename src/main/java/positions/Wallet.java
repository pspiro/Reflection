package positions;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import tw.util.S;

/** Get token positions */
public class Wallet {
	static String test = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";  // for testing only
	
	private String m_address;
	private HashMap<String, Double> m_map; // map token (lower case) to balance
	
	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_address = address;
	}

	/** Only send the request the first time
	 *  @param token must be lower case */ 
	public double getBalance(String token) throws Exception {
		if (m_map == null) {
			m_map = reqPositionsMap(m_address);
		}
		Double val = m_map.get(token);
		return val != null ? val : 0.;
	}

	/** Returns a map of contract address (lower case) -> position (Double) */ 
	public static HashMap<String,Double> reqPositionsMap(String wallet) throws Exception {
		Util.reqValidAddress(wallet);
		HashMap<String,Double> map = new HashMap<>();
		
		for (JsonObject token : MoralisServer.reqPositionsList(wallet) ) {
			String addr = token.getString("token_address");			
			String balance = token.getString("balance");
			if (S.isNotNull(addr) && S.isNotNull(balance) ) {
				map.put( addr.toLowerCase(), Erc20.fromBlockchain(balance, token.getInt("decimals") ) );
			}
		}
		
		return map;
	}

	public double getNativeTokenBalance() throws Exception {
		return MoralisServer.getNativeBalance(m_address);
	}
	
}
