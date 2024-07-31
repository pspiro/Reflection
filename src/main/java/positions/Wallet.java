package positions;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.Erc20;

/** Get token positions; will only send one query */
public class Wallet {
	private String m_walletAddr;
	
	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_walletAddr = address;
	}
	
	public String walletAddr() {
		return m_walletAddr;
	}

	/** Sends a request every time
	 *  @deprecated use Erc20.getPosition() instead
	 *  @param token is token address */
	public double getBalance(String token) throws Exception {
		return getBalance( reqPositionsMap(m_walletAddr, token), token);
	}

	/** Look up the value in the map and convert to decimal;
	 *  Use this one if you want more than one token balance */
	public static double getBalance(HashMap<String,Double> map, String token) throws Exception {
		return Util.toDouble( map.get(token.toLowerCase() ) );
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
	
	/** Returns a map of contract address (lower case) -> position (Double)
	 *  This version works by looking at all token transfers for the wallet
	    this doesn't work because mint and burn transfers are not returned
		supposedly being fixed "by end of Q1" */
//	public static HashMap<String,Double> reqPositionsMap(String address) throws Exception {
//		HashMap<String,Double> map = new HashMap<>();
//
//		// get all transactions in batches and build the map
//		MoralisServer.getAllWalletTransfers(address, ar -> ar.forEach( obj -> {
//				String tokenAddress = obj.getString("address").toLowerCase();
//				double amt = obj.getDouble("value_decimal");
//
//				// note that the wallet could be from, to, or both
//				if (obj.getString("from_address").equalsIgnoreCase(address) ) {
//					Erc20.inc( map, tokenAddress, -amt);
//				}
//				
//				if (obj.getString("to_address").equalsIgnoreCase(address) ) {
//					Erc20.inc( map, tokenAddress, amt);
//				}
//		} ) );
//		
//		Util.filter( map, val -> Math.abs(val) >= .000001);  // remove items with tiny balance
//		return map;
//	}

	/** Sends a new query every time */
	public static double getBalance(String wallet, String tokenAddr) throws Exception {
		return new Wallet(wallet).getBalance(tokenAddr);
	}
	
	public double getNativeBalance() throws Exception {
		return MoralisServer.getNativeBalance(m_walletAddr);
	}
	
}
