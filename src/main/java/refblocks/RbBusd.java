package refblocks;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import common.Util;
import fireblocks.RetVal;
import web3.Busd.IBusd;
import web3.Erc20;

/** Implements the Busd contract methods that are writable, and deploy() */
public class RbBusd extends Erc20 implements IBusd {
	public RbBusd( String address, int decimals, String name) {
		super( address, decimals, name);
	}
	
	public static String deploy(String ownerKey) throws Exception {
		return Busd.deploy( 
				Refblocks.web3j,
				Refblocks.getTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas)
				).send().getContractAddress();
	}

	/** load generated Busd that we can use to call smart contract methods that write to the blockchain */
	public Busd loadBusd(String privateKey) throws Exception {
		return Busd.load( 
				address(), 
				Refblocks.web3j, 
				Refblocks.getTm( privateKey ), 
				Refblocks.getGp( 1000000)
				);
	}

	/** For testing only 
	 * @throws Exception */
	@Override public RetVal approve(String approverKey, String spenderAddr, double amt) throws Exception {
		Util.isValidKey(approverKey);
		Util.isValidAddress(spenderAddr);
		
		TransactionReceipt rec = loadBusd( approverKey)
				.approve( spenderAddr, toBlockchain( amt) )
				.send();
		
		Refblocks.showReceipt( rec);


		return null;
	}

	@Override public RetVal mint( String callerKey, String address, double amount) throws Exception {
		Util.isValidKey(callerKey);
		Util.isValidAddress(address);

		TransactionReceipt rec = loadBusd( callerKey)
				.mint( address, toBlockchain( amount) )
				.send();
		
		Refblocks.showReceipt( rec);
		
		return null;
	}

}
