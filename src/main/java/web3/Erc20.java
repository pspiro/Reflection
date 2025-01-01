package web3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import chain.Chain;
import common.Util;
import refblocks.Refblocks;
import tw.util.S;
import web3.Param.Address;
import web3.Param.BigInt;

/** Base class for the generic tokens AND ALSO the platform-specific tokens */
public class Erc20 {
	protected static final BigDecimal ten = new BigDecimal(10);
	private static final String totalSupplyAbi = Util.easyJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");
	
	// keccaks - write   (	// 'address' and 'uint256' to calculate keccak)
	public static String Approve = "0x095ea7b3";
	public static String TransferOwnership = "0xf2fde38b";
	public static String Mint = "0x40c10f19";
	public static String TransferFrom = "0x23b872dd";
	public static String Transfer = "0xa9059cbb";

	protected String m_address;
	protected int m_decimals;
	protected String m_name;
	protected Chain m_chain;

	protected Erc20( String address, int decimals, String name, Chain chain) {
		m_address = address;
		m_decimals = decimals;
		m_name = name;
		m_chain = chain;
		
		m_chain.node().setDecimals( m_decimals, m_address);
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
		return m_chain.node().getBalance( m_address, walletAddr, m_decimals);
	}

	/** return the balances of all wallets holding this token;
	 *  Used by Monitor and ProofOfReserves only, not any core apps
	 *  
	 *  PulseChain uses a different method
	 *  
	 * @return map wallet address -> token balance */
	public HashMap<String,Double> getAllBalances() throws Exception {
		HashMap<String,Double> map = new HashMap<>();
		
		if (m_chain.params().isPolygon() ) {
			
			// get all transactions in batches and build the map
			MoralisServer.setChain( m_chain.params().moralisPlatform() ); // NOT SAFE
			MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
					Util.wrap( () -> {
						double value = fromBlockchain( obj.getString("value") );
						Util.inc( map, obj.getString("from_address"), -value);
						Util.inc( map, obj.getString("to_address"), value);
					});
			} ) );
		}
		else {
			// get all transactions, build the map
			for (var transfer : m_chain.node().getAllTokenTransfers( m_address, m_decimals) ) {
				Util.inc( map, transfer.from(), -transfer.amount() );
				Util.inc( map, transfer.to(), transfer.amount() );
			}
		}
		
		return map;
	}

	/** note w/ moralis you can also get the token balance by wallet */
	public double queryTotalSupply() throws Exception {
		return m_chain.node().getTotalSupply( m_address, m_decimals);
	}

	/** Sends a query to Moralis */
	public double getAllowance( String approverAddr, String spender) throws Exception {
		return fromBlockchain( 
				m_chain.node().getAllowance( m_address, approverAddr, spender) );
	}
	
	public String getOwner() throws Exception {
		return m_chain.node().getOwner( m_address);
	}

	public RetVal setOwner( String ownerKey, String newOwnerAddr) throws Exception {
		return m_chain.node().callSigned(
				ownerKey,
				m_address,
				TransferOwnership,
				Util.toArray( new Address( newOwnerAddr) ),  
				500000); 
	}
	
	public RetVal approve( String ownerKey, String spender, double amount) throws Exception {
		Param[] params = {
				new Address( spender),
				new BigInt( toBlockchain( amount) )
		};

		S.out( "Erc20: let %s ('owner') approve %s ('spender') to spend %s %s",
				Util.getAddress( ownerKey), spender, amount, m_name);
		
		return m_chain.node().callSigned(
				ownerKey,
				m_address,
				Approve,
				params,
				500000); 
	}
	
	public RetVal mint( String callerKey, String address, double amount) throws Exception {
		Param[] params = {
				new Address( address),
				new BigInt( toBlockchain( amount) )
		};

		S.out( "Erc20: let %s mint %s %s into %s",
				Util.getAddress( callerKey), amount, m_name, address);
		
		return m_chain.node().callSigned(
				callerKey,
				m_address,
				Mint,
				params,
				Refblocks.deployGas); 
	}
	
	public RetVal transfer(String fromKey, String toAddr, double amount) throws Exception {
		Param[] params = {
				new Address( toAddr),
				new BigInt( toBlockchain( amount) )
		};

		S.out( "Erc20: transfer %s %s from %s to %s",
				amount, m_name, Util.getAddress( fromKey), toAddr);
		
		return m_chain.node().callSigned(
				fromKey,
				m_address,
				Transfer,
				params,
				500000); 
	}
	
	/** this does not work and I don't know why; gives:
	 * 
	 * invalid argument 0: json: cannot unmarshal invalid hex string into Go struct field 
	 * TransactionArgs.data of type hexutil.Bytes
	 */
	public RetVal transferFrom(String spenderKey, String fromAddr, String toAddr, double amount) throws Exception {
		Param[] params = {
				new Address( fromAddr),
				new Address( toAddr),
				new BigInt( toBlockchain( amount) )
		};

		S.out( "Erc20: let %s transfer %s %s from %s to %s",
				Util.getAddress( spenderKey), amount, m_name, fromAddr, toAddr);
		
		return m_chain.node().callSigned(
				spenderKey,
				m_address,
				TransferFrom,
				params,
				500000); 
	}
}
