package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import positions.MoralisServer;
import positions.Wallet;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Erc20 {
	static final String approveKeccak = "095ea7b3";
	static final String totalSupplyAbi = Util.toJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");

	protected String m_address;
	protected int m_decimals;
	
	Erc20( String address, int decimals) {
		this.m_address = address;
		this.m_decimals = decimals;
	}
	
	public String address() {
		return m_address;
	}
	
	public int decimals() {
		return m_decimals;
	}
	
	/** Approve some wallet to spend on behalf of another
	 *  NOTE: you must wait for the response */
	RetVal approve(int accountId, String spenderAddr, double amt) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				spenderAddr, 
				toBlockchain( amt), 
			};
		
		S.out( "Account %s approving %s to spend %s %s", accountId, spenderAddr, amt, m_address);
		return Fireblocks.call2( accountId, m_address, 
				Rusd.approveKeccak, paramTypes, params, "BUSD approve");
		
	}

	/** Return amt rounded to four decimals * 10^power */
	static final BigDecimal ten = new BigDecimal(10);

	static BigInteger timesPower(double amt, int power) {
		return new BigDecimal( S.fmt4( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}

	/** Returns hex string */
	public BigInteger toBlockchain(double amt) throws RefException {
		return timesPower( amt, m_decimals); 
	}
	
	/** Takes decimal string */
	public double fromBlockchain(String amt) {
		return fromBlockchain( amt, m_decimals);
	}
	
	public static double fromBlockchain(String amt, int power) {
		return S.isNotNull(amt)
				? new BigDecimal(amt).divide( ten.pow(power) ).doubleValue()
				: 0.0;
	}
	
	public static double fromBlockchainHex(String amt, int power) {
		return new BigDecimal( new BigInteger(amt,16) )
				.divide( ten.pow(power) )
				.doubleValue(); 
	}
	
	/** Sends a query to Moralis */
	public double getAllowance(String wallet, String spender) throws Exception {
		return fromBlockchain( MoralisServer.reqAllowance(m_address, wallet, spender).getString("allowance") );
	}

	/** Returns the number of this token held by wallet; sends a query to Moralis
	 *  If you need multiple positions from the same wallet, use Wallet class instead */ 
	public double getPosition(String walletAddr) throws Exception {
		return new Wallet(walletAddr).getBalance(m_address); 
	}
	
	public double queryTotalSupply() throws Exception {
		String supply = MoralisServer.contractCall( m_address, "totalSupply", totalSupplyAbi);		
		Util.require( supply != null, "Moralis total supply returned null for " + m_address);
		return fromBlockchain(
				supply.replaceAll("\"", ""), // strip quotes
				m_decimals);
	}
}
