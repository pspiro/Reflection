package web3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import common.Util;
import tw.util.S;

/** Base class for the generic tokens AND ALSO the platform-specific tokens */
public class Erc20 {
	protected static final BigDecimal ten = new BigDecimal(10);
	private static final String totalSupplyAbi = Util.easyJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");
	
//	totalSupply:   '0x18160ddd'
//	balanceOf:     '0x70a08231'
//	transfer:      '0xa9059cbb'
//	transferFrom:  '0x23b872dd'
//	approve:       '0x095ea7b3'
//	allowance:     '0xdd62ed3e'
//	decimals:      '0x313ce567'	

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

	/** Takes decimal string 
	 * @throws Exception */
	public double fromBlockchain(String amt) throws Exception {
		return fromBlockchain( amt, m_decimals);
	}
	
	/** Can take hex or decimal. '0x' is invalid, returns error.
	 *  You could create another version that allows empty string or pass param
	 *  
	 * Do NOT attempt to use Numeric.decodeQuantity(); it has limitations
	 * owing to using long for the conversion */
    public static BigInteger decodeQuantity(String value) throws Exception {
    	try {
	    	return value.startsWith( "0x")
	    		? new BigInteger( value.substring( 2), 16)
	    		: new BigInteger( value);
    	}
    	catch( Exception e) {
            S.out( "Could not parse number '%s'", value);
            throw e;
    	}
    }

	/** Can take decimal or hex; should really throw an exception 
	 * @throws Exception */
	public static double fromBlockchain(String amt, int decimals) throws Exception {
		Util.require( decimals > 0, "decimals cannot be zero");
		
		try {
			return S.isNotNull(amt)
					? new BigDecimal( decodeQuantity(amt) )
							.divide( ten.pow(decimals) )
							.doubleValue()
					: 0.0;
		}
		catch( Exception e) {
			S.out( "Error: cannot decode " + amt);
			throw e;
		}
	}
	
	public BigInteger toBlockchain(double amt) throws Exception {
		return toBlockchain( amt, m_decimals); 
	}

	/** Return amt rounded to four decimals * 10^power 
	 * @throws Exception */
	public static BigInteger toBlockchain(double amt, int decimals) throws Exception {
		Util.require( decimals > 0, "decimals cannot be zero");

		return new BigDecimal( S.fmt4( amt) )
				.multiply( ten.pow( decimals) )
				.toBigInteger();
	}
	
	/** Returns the number of this token held by wallet; sends a query to Moralis
	 *  If you need multiple positions from the same wallet, use Wallet class instead */ 
	public double getPosition(String walletAddr) throws Exception {
		return NodeServer.getBalance( m_address, walletAddr, m_decimals);
	}

	/** return the balances of all wallets holding this token;
	 *  Used by Monitor and ProofOfReserves only, not any core apps
	 *  
	 *  PulseChain uses a different method
	 *  
	 * @return map wallet address -> token balance */
	public HashMap<String,Double> getAllBalances() throws Exception {
		HashMap<String,Double> map = new HashMap<>();

		// get all transactions in batches and build the map
		MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
				Util.wrap( () -> {
					double value = fromBlockchain( obj.getString("value") );
					Util.inc( map, obj.getString("from_address"), -value);
					Util.inc( map, obj.getString("to_address"), value);
				});
		} ) );
		
		return map;
	}

	/** note w/ moralis you can also get the token balance by wallet */
	public double queryTotalSupply() throws Exception {
		return NodeServer.getTotalSupply( m_address, m_decimals);
	}

	/** Sends a query to Moralis */
	public double getAllowance(String approverAddr, String spender) throws Exception {
		return NodeServer.getAllowance( m_address, approverAddr, spender, m_decimals);
	}
}
