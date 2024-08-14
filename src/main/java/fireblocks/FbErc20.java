package fireblocks;

import org.json.simple.JsonObject;

import common.Util;
import reflection.RefCode;
import reflection.RefException;
import tw.util.IStream;
import tw.util.S;
import web3.Erc20;
import web3.MoralisServer;
import web3.RetVal;

public class FbErc20 extends Erc20 {
	public static final int DECIMALS = 4; // must match the # of decimals in timesPower() below;
										  // for stock tokens, this might not be enough
	static final String approveKeccak = "095ea7b3";
	static final String mintKeccak = "40c10f19";
	static final String burnKeccak = "9dc29fac";
	static final String transOwnerKeccak = "f2fde38b";

	public FbErc20( String address, int decimals, String name) throws Exception {
		super( address, decimals, name);
		
		Util.require( 
				S.isNull(address) || 
				address.equalsIgnoreCase("deploy") || 
				Util.isValidAddress(address), "Invalid Erc20 address");
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
				FbRusd.approveKeccak, paramTypes, params, "Stablecoin approve");
		
	}

	/** Sends a query to Moralis */
//	public double getAllowance(String wallet, String spender) throws Exception {
//		Util.reqValidAddress(wallet);
//		return fromBlockchain( MoralisServer.reqAllowance(m_address, wallet, spender).getString("allowance") );
//	}
//
//	/** Returns the number of this token held by wallet; sends a query to Moralis
//	 *  If you need multiple positions from the same wallet, use Wallet class instead */ 
//	public double getPosition(String walletAddr) throws Exception {
//		Util.reqValidAddress(walletAddr);
//		return new Wallet(walletAddr).getBalance(m_address); 
//	}


	/** The wallet associated w/ ownerAcctId becomes the owner of the deployed contract.
	 *  The parameters passed here are the passed to the constructor of the smart contract
	 *  being deployed. The whole thing takes 30 seconds.
	 *  @return the deployed contract address */
	protected static String deploy(String filename, int ownerAcctId, String[] paramTypes, Object[] params, String note) throws Exception {
		S.out( "Deploying contract from %s", filename);
		
		// very strange, sometimes we get just the bytecode, sometimes we get a json object
		String bytecode = JsonObject.parse( IStream.readAll(filename) )
				.getString("bytecode");
		Util.require( S.isNotNull(bytecode) && bytecode.toLowerCase().startsWith("0x"), "Invalid bytecode" );
//		String bytecode = new IStream(filename).readln();
		
		S.out( "  waiting for blockchain transaction hash");
		String txHash = Fireblocks.call2( ownerAcctId, "0x0", bytecode, paramTypes, params, note)
				.waitForHash();
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
	
	public RetVal call(int fromAcct, String callData, String[] paramTypes, Object[] params, String note) throws Exception {
		return Fireblocks.call2(fromAcct, m_address, callData, paramTypes, params, note);
	}

//	public static RetVal mint(int fromAcct, String address, double amt) throws Exception {
//	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal mint(int fromAcct, String toAddress, double amt) throws Exception {
		S.out( "Minting %s %s for %s", amt, m_name, fromAcct);
		
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				toAddress, 
				toBlockchain( amt) 
		};
		
		return call( fromAcct, mintKeccak, paramTypes, params, "ERC20 mint");
	}

	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal burn(int fromAcct, String address, double amt) throws Exception {
		S.out( "Burning %s %s for %s", amt, m_name, fromAcct);
		
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				address, 
				toBlockchain( amt) 
		};
		
		return call( fromAcct, burnKeccak, paramTypes, params, "Stablecoin burn");
	}
	
	public RetVal setOwner( int fromAcct, String ownerAddr) throws Exception {
		S.out( "Setting owner on %s to %s", m_address, ownerAddr);
		
		String[] paramTypes = { "address" };
		
		Object[] params = { 
				ownerAddr, 
		};
		
		return call( fromAcct, transOwnerKeccak, paramTypes, params, "Transfer ownership");
	}
}
