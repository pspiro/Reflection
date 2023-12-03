package positions;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Erc20;
import reflection.Config;
import tw.util.S;

/** Get token positions; will only send one query */
public class Wallet {
	private String m_address;
	private HashMap<String, Double> m_map; // map token (lower case) to balance
	
	public static void main(String[] args) throws Exception {
		Config.ask();
		String addr = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce"; //Accounts.instance.getAddress("Peter Spiro");
		Wallet wallet = new Wallet(addr);
		wallet.showAll();
	}
	
	private void showAll() throws Exception {
		if (m_map == null) {
			m_map = reqPositionsMap(m_address);
		}
		S.out(m_map);
	}

	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_address = address;
	}

	/** Only send the request the first time */
	public double getBalance(String token) throws Exception {
		if (m_map == null) {
			m_map = reqPositionsMap(m_address);
		}
		Double val = m_map.get(token.toLowerCase() );
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
	
	/** Sends a new query every time */
	public static double getBalance(String wallet, String tokenAddr) throws Exception {
		return new Wallet(wallet).getBalance(tokenAddr);
	}
	
}
