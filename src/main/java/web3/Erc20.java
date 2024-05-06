package web3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util;
import positions.MoralisServer;
import positions.Wallet;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

public class Erc20 {
	public static final int DECIMALS = 4; // must match the # of decimals in timesPower() below;
										  // for stock tokens, this might not be enough
	static final String approveKeccak = "095ea7b3";
	static final String mintKeccak = "40c10f19";
	static final String burnKeccak = "9dc29fac";
	static final String totalSupplyAbi = Util.easyJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");
	static final BigDecimal ten = new BigDecimal(10);

	protected String m_address;
	protected int m_decimals;
	private String m_name;
	
	public Erc20( String address, int decimals, String name) throws Exception {
		Util.require( 
				S.isNull(address) || 
				address.equalsIgnoreCase("deploy") || 
				Util.isValidAddress(address), "Invalid Erc20 address");
		
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
	
	
	/** Approve some wallet to spend on behalf of another
	 *  NOTE: you must wait for the response */
//	public RetVal approve(int accountId, String spenderAddr, double amt) throws Exception {
//	}

	/** Return amt rounded to four decimals * 10^power */
	static BigInteger timesPower(double amt, int power) {
		return new BigDecimal( S.fmt4( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}

	/** Returns hex string
	 *  @param amt is rounded to four decimals; this is fine as long as the frontend is 
	 *  rounding to <= 4 decimals */
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
		Util.reqValidAddress(wallet);
		return fromBlockchain( MoralisServer.reqAllowance(m_address, wallet, spender).getString("allowance") );
	}

	/** Returns the number of this token held by wallet; sends a query to Moralis
	 *  If you need multiple positions from the same wallet, use Wallet class instead */ 
	public double getPosition(String walletAddr) throws Exception {
		Util.reqValidAddress(walletAddr);
		return new Wallet(walletAddr).getBalance(m_address); 
	}

	/** note w/ moralis you can also get the token balance by wallet */
	public double queryTotalSupply() throws Exception {
		String supply = MoralisServer.contractCall( m_address, "totalSupply", totalSupplyAbi);		
		Util.require( supply != null, "Moralis total supply returned null for " + m_address);
		return fromBlockchain(
				supply.replaceAll("\"", ""), // strip quotes
				m_decimals);
	}

	/** The wallet associated w/ ownerAcctId becomes the owner of the deployed contract.
	 *  The parameters passed here are the passed to the constructor of the smart contract
	 *  being deployed. The whole thing takes 30 seconds.
	 *  @return the deployed contract address */
//	protected static String deploy(String filename, int ownerAcctId, String[] paramTypes, Object[] params, String note) throws Exception {
//	}

	/** Query the blockchain transaction through Moralis until the transaction
	 *  is there AND it contains the receipt_contract_address field;
	 *  takes about 17 seconds. */
	public static String getDeployedAddress(String txHash) throws Exception {
		for (int i = 0; i < 3*60; i++) {
			if (i > 0) S.sleep(1000);
			try {
				
				S.out( "    querying...");
				JsonObject obj = MoralisServer.queryTransaction(txHash);
				String addr = obj.getString("receipt_contract_address");
				if (S.isNotNull(addr) ) {
					S.out( "contract deployed to " + addr);
					return addr;
				}
			}
			catch( Exception e) {
				if (S.notNull(e.getMessage()).contains("404") ) {
					// swallow it
				}
				else {
					S.err( "Error while querying for deployed address", e);
				}
			}
		}
		throw new RefException( RefCode.UNKNOWN, "Could not get blockchain transaction");		
	}
	
//	/** This can be called by anybody, the BUSD does not have an owner.
//	 *  For testing only; cannot be called in production */
//	public RetVal mint(int fromAcct, String address, double amt) throws Exception {
//	}
//
//	/** This can be called by anybody, the BUSD does not have an owner.
//	 *  For testing only; cannot be called in production */
//	public RetVal burn(int fromAcct, String address, double amt) throws Exception {
//	}

	/** print out the balances of all wallets holding this token
	 * @return map wallet address -> token balance */
	public HashMap<String,Double> getAllBalances() throws Exception {
		HashMap<String,Double> map = new HashMap<>();

		// get all transactions in batches and build the map
		MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
				double value = Erc20.fromBlockchain( obj.getString("value"), m_decimals);  // use value_decimal here
				inc( map, obj.getString("from_address"), -value);
				inc( map, obj.getString("to_address"), value);
		} ) );
		
		return map;
	}

	public void showAllTransactions() throws Exception {
			MoralisServer.getAllTokenTransfers(m_address, ar -> ar.forEach( obj -> {
				S.out( "%8s %s %s %s", 
						obj.getString("value_decimal"), 
						Util.left( obj.getString("from_address"), 8),
						Util.left( obj.getString("to_address"), 8),
						obj.getString("transaction_hash") );
		} ) );
	}
	
	public static void inc(HashMap<String, Double> map, String address, double amt) {
		Double v = map.get(address);
		map.put( address, v == null ? amt : v + amt);
	}
}
