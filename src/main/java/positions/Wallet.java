package positions;

import java.util.HashMap;

import common.Util;
import fireblocks.Erc20;
import reflection.Config;

/** Get token positions; will only send one query */
public class Wallet {
	private String m_address;
	private HashMap<String, Double> m_map; // map token address (lower case) to balance
	
	private void showAll() throws Exception {
		if (m_map == null) {
			m_map = reqPositionsMap(m_address);
		}
		Util.show( m_map);
	}

	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_address = address;
	}

	/** Only send the request the first time
	 *  @param token is token address */
	public double getBalance(String token) throws Exception {
		if (m_map == null) {
			m_map = reqPositionsMap(m_address);
		}
		Double val = m_map.get(token.toLowerCase() );
		return val != null ? val : 0.;
	}

	/** Returns a map of contract address (lower case) -> position (Double).
	 *  This version retrieves the map from Moralis is is having issues of
	 *  missing positions as of 1/23/24 */ 
//	public static HashMap<String,Double> reqPositionsMap(String wallet) throws Exception {
//		Util.reqValidAddress(wallet);
//		HashMap<String,Double> map = new HashMap<>();
//		
//		for (JsonObject token : MoralisServer.reqPositionsList(wallet) ) {
//			String addr = token.getString("token_address");			
//			String balance = token.getString("balance");
//			if (S.isNotNull(addr) && S.isNotNull(balance) ) {
//				map.put( addr.toLowerCase(), Erc20.fromBlockchain(balance, token.getInt("decimals") ) );
//			}
//		}
//		
//		return map;
//	}
	
	/** Returns a map of contract address (lower case) -> position (Double)
	 *  This version works by looking at all token transfers for the wallet
	 *  It is reliable */ 
	public static HashMap<String,Double> reqPositionsMap(String address) throws Exception {
		HashMap<String,Double> map = new HashMap<>();

		// get all transactions in batches and build the map
		MoralisServer.getAllWalletTransfers(address, ar -> ar.forEach( obj -> {
				String tokenAddress = obj.getString("address").toLowerCase();
				double amt = obj.getDouble("value_decimal");

				// note that the wallet could be from, to, or both
				if (obj.getString("from_address").equalsIgnoreCase(address) ) {
					Erc20.inc( map, tokenAddress, -amt);
				}
				
				if (obj.getString("to_address").equalsIgnoreCase(address) ) {
					Erc20.inc( map, tokenAddress, amt);
				}
		} ) );
		
		Util.filter( map, val -> Math.abs(val) >= .000001);  // remove items with tiny balance
		return map;
	}

	public double getNativeTokenBalance() throws Exception {
		return MoralisServer.getNativeBalance(m_address);
	}
	
	/** Sends a new query every time */
	public static double getBalance(String wallet, String tokenAddr) throws Exception {
		return new Wallet(wallet).getBalance(tokenAddr);
	}
	
	public static void main(String[] args) throws Exception {
		Config.ask();
		String addr = "0xa14749d89e1ad2a4de15ca4463cd903842ffc15d"; //Accounts.instance.getAddress("Peter Spiro");
		//String addr = "0x2703161d6dd37301ced98ff717795e14427a462b"; //Accounts.instance.getAddress("Peter Spiro");

 		Wallet wallet = new Wallet(addr);
		wallet.showAll();
	}
}
