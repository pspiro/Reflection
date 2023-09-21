package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.simple.JsonObject;

import common.Util;
import positions.MoralisServer;
import positions.Wallet;
import reflection.RefCode;
import reflection.RefException;
import tw.util.IStream;
import tw.util.S;

public class Erc20 {
	public static final int DECIMALS = 4; // must match the # of decimals in timesPower() below;
										  // for stock tokens, this might not be enough
	static final String approveKeccak = "095ea7b3";
	static final String totalSupplyAbi = Util.fmtJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");

	protected String m_address;
	protected int m_decimals;
	
	Erc20( String address, int decimals) throws Exception {
		Util.reqValidAddress(address);
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
	public RetVal approve(int accountId, String spenderAddr, double amt) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				spenderAddr, 
				toBlockchain( amt), 
			};
		
		S.out( "Account %s approving %s to spend %s %s", accountId, spenderAddr, amt, m_address);
		return Fireblocks.call2( accountId, m_address, 
				Rusd.approveKeccak, paramTypes, params, "Stablecoin approve");
		
	}

	/** Return amt rounded to four decimals * 10^power */
	static final BigDecimal ten = new BigDecimal(10);

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
	public static String deploy(String filename, int ownerAcctId, String[] paramTypes, Object[] params, String note) throws Exception {
		S.out( "Deploying contract from %s", filename);
		
		// very strange, sometimes we get just the bytecode, sometimes we get a json object
		String bytecode = JsonObject.parse( new IStream(filename).readAll() )
				.getString("bytecode");
		Util.require( S.isNotNull(bytecode) && bytecode.toLowerCase().startsWith("0x"), "Invalid bytecode" );
//		String bytecode = new IStream(filename).readln();
		
		String id = Fireblocks.call( ownerAcctId, "0x0", bytecode, paramTypes, params, note);
		
		// if there's an error, you got message and code
		
		//{"message":"Source is invalid","code":1427}		
		
		// it takes 30 seconds to deploy a contract and get the contract address back; how long does it take from javascript?
		S.out( "  fireblocks id is %s", id);

		S.out( "  waiting for blockchain transaction hash");
		String txHash = Fireblocks.waitForTransHash( id, 60, 1000);
		S.out( "  blockchain transaction hash is %s", txHash);

		S.out( "  waiting for deployed address");
		return getDeployedAddress(txHash);
	}

	/** Query the blockchain transaction through Moralis until the transaction
	 *  is there AND it contains the receipt_contract_address field;
	 *  takes about 17 seconds. */
	public static String getDeployedAddress(String txHash) throws Exception {
		for (int i = 0; i < 3*60; i++) {
			if (i > 0) S.sleep(1000);
			
			S.out( "    querying...");
			JsonObject obj = MoralisServer.queryTransaction(txHash, Fireblocks.moralisPlatform);
			String addr = obj.getString("receipt_contract_address");
			if (S.isNotNull(addr) ) {
				S.out( "contract deployed to " + addr);
				return addr;
			}
		}
		throw new RefException( RefCode.UNKNOWN, "Could not get blockchain transaction");		
	}
}
