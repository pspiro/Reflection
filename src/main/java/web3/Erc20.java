package web3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import common.Util;
import positions.Wallet;
import refblocks.Refblocks;
import tw.util.S;

/** Base class for the generic tokens AND ALSO the platform-specific tokens */
public class Erc20 {
	protected static final BigDecimal ten = new BigDecimal(10);
	private static final String totalSupplyAbi = Util.easyJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");

	protected String m_address;
	protected int m_decimals;
	protected String m_name;

	protected Erc20( String address, int decimals, String name) {
		m_address = address;
		m_decimals = decimals;
		m_name = name;
	}
	
	public String address() {
		return m_address;
	}
	
	public int decimals() {
		return m_decimals;
	}
	
	public String name() {
		return m_name;
	}

	/** Takes decimal string */
	public double fromBlockchain(String amt) {
		return fromBlockchain( amt, m_decimals);
	}
	
	/** Can take decimal or hex */
	public static double fromBlockchain(String amt, int power) {
		return S.isNotNull(amt)
				? new BigDecimal( Refblocks.decodeQuantity( amt) )
						.divide( ten.pow(power) )
						.doubleValue()
				: 0.0;
	}
	
	public BigInteger toBlockchain(double amt) {
		return toBlockchain( amt, m_decimals); 
	}

	/** Return amt rounded to four decimals * 10^power */
	public static BigInteger toBlockchain(double amt, int power) {
		return new BigDecimal( S.fmt4( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}
	
	/** Returns the number of this token held by wallet; sends a query to Moralis
	 *  If you need multiple positions from the same wallet, use Wallet class instead */ 
	public double getPosition(String walletAddr) throws Exception {
		return Refblocks.getERC20Balance( walletAddr, m_address, m_decimals); 
	}

	/** return the balances of all wallets holding this token
	 * @return map wallet address -> token balance */
	public HashMap<String,Double> getAllBalances() throws Exception {
		HashMap<String,Double> map = new HashMap<>();

		// get all transactions in batches and build the map
		MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
				double value = fromBlockchain( obj.getString("value") );  // could use value_decimal here
				inc( map, obj.getString("from_address"), -value);
				inc( map, obj.getString("to_address"), value);
		} ) );
		
		return map;
	}

	/** Look up value by address and increment it */
	public static void inc(HashMap<String, Double> map, String address, double amt) {
		Double v = map.get(address);
		map.put( address, v == null ? amt : v + amt);
	}

	/** not used */
	public void showAllTransactions() throws Exception {
			MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
				S.out( "%8s %s %s %s", 
						obj.getString("value_decimal"), 
						Util.left( obj.getString("from_address"), 8),
						Util.left( obj.getString("to_address"), 8),
						obj.getString("transaction_hash") );
		} ) );
	}
	
	/** note w/ moralis you can also get the token balance by wallet */
	public double queryTotalSupply() throws Exception {
		String supply = MoralisServer.contractCall( m_address, "totalSupply", totalSupplyAbi);		
		Util.require( supply != null, "Moralis total supply returned null for " + m_address);
		return fromBlockchain(
				supply.replaceAll("\"", ""), // strip quotes
				m_decimals);
	}

	/** Sends a query to Moralis */
	public double getAllowance(String wallet, String spender) throws Exception {
		Util.reqValidAddress(wallet);
		return fromBlockchain( MoralisServer.reqAllowance(m_address, wallet, spender).getString("allowance") );
	}
}
