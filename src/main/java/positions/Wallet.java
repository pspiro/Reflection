package positions;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import reflection.Config;
import tw.util.S;

/** Get token positions; will only send one query */
public class Wallet {
	private String m_walletAddr;
	
	public Wallet(String address) throws Exception {
		Util.reqValidAddress(address);
		m_walletAddr = address;
	}

	/** Sends a request every time
	 *  @param token is token address */
	public double getBalance(String token) throws Exception {
		return Util.toDouble( 
				reqPositionsMap(m_walletAddr, token).get(token.toLowerCase() ) 
		);
	}

	/** Returns a map of contract address (lower case) -> position (Double).
	 *  This version retrieves the map from Moralis is is having issues of
	 *  missing positions as of 1/23/24 
	 *  I think passing the contracts may fix it */ 
	public HashMap<String,Double> reqPositionsMap(String... contracts) throws Exception {
		Util.require( contracts.length > 0, "Contract addresses are required");  // needed to to Moralis bug
		
		HashMap<String,Double> map = new HashMap<>();
		
		for (JsonObject token : MoralisServer.reqPositionsList(m_walletAddr, contracts) ) {
			String addr = token.getString("token_address");			
			String balance = token.getString("balance");
			if (S.isNotNull(addr) && S.isNotNull(balance) ) {
				map.put( addr.toLowerCase(), Erc20.fromBlockchain(balance, token.getInt("decimals") ) );
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

	public double getNativeTokenBalance() throws Exception {
		return MoralisServer.getNativeBalance(m_walletAddr);
	}
	
	/** Sends a new query every time */
	public static double getBalance(String wallet, String tokenAddr) throws Exception {
		return new Wallet(wallet).getBalance(tokenAddr);
	}
	
	public static void main(String[] args) throws Exception {
		Config.ask();
		String wal = "0xa14749d89e1ad2a4de15ca4463cd903842ffc15d"; //Accounts.instance.getAddress("Peter Spiro");
		//String addr = "0x2703161d6dd37301ced98ff717795e14427a462b"; //Accounts.instance.getAddress("Peter Spiro");
		String rusd = "0x455759a3f9124bf2576da81fb9ae8e76b27ff2d6";

		HashMap<String, Double> map = new Wallet(wal).reqPositionsMap(rusd);
		Util.show(map);
	}
}
