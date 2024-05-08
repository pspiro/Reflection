package refblocks;

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
	public Busd load(String privateKey) throws Exception {
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
		Util.reqValidKey(approverKey);
		Util.reqValidAddress(spenderAddr);
		
		return Refblocks.oldexec( approverKey, load( approverKey)
				.approve( spenderAddr, toBlockchain( amt) ) );
	}
		
	/** For testing only; anyone can call this but they must have some gas */
	@Override public RetVal mint( String callerKey, String address, double amount) throws Exception {
		Util.reqValidKey(callerKey);
		Util.reqValidAddress(address);

		return Refblocks.oldexec( callerKey, load( callerKey)
				.mint( address, toBlockchain( amount) ) );
	}
}
