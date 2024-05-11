package refblocks;

import org.web3j.tx.TransactionManager;

import common.Util;
import tw.util.S;
import web3.Busd.IBusd;
import web3.Erc20;
import web3.RetVal;

/** Implements the Busd contract methods that are writable, and deploy() */
public class RbBusd extends Erc20 implements IBusd {
	public RbBusd( String address, int decimals, String name) {
		super( address, decimals, name);
	}
	
	/** note that the number of decimals is set in the .sol file
	 *  before the Busd file is generaged */
	public static String deploy(String ownerKey) throws Exception {
		return Busd.deploy( 
				Refblocks.web3j,
				Refblocks.getWaitingTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas)
				).send().getContractAddress();
	}

	/** load generated Busd that we can use to call smart contract methods that write to the blockchain */
	public Busd load(TransactionManager tm, int gas) throws Exception {
		return Busd.load( 
				address(), 
				Refblocks.web3j, 
				tm, 
				Refblocks.getGp( gas)
				);
	}

	/** For testing only 
	 * @throws Exception */
	@Override public RetVal approve(String approverKey, String spenderAddr, double amt) throws Exception {
		Util.reqValidKey(approverKey);
		Util.reqValidAddress(spenderAddr);
		
		S.out( "%s approving %s to spend %s BUSD", 
				Refblocks.getAddressPk(approverKey), spenderAddr, amt);
		
		return Refblocks.exec( approverKey, tm -> load( tm, 100000)
				.approve( spenderAddr, toBlockchain( amt) ) );
	}
		
	/** For testing only; anyone can call this but they must have some gas */
	@Override public RetVal mint( String callerKey, String address, double amt) throws Exception {
		Util.reqValidKey(callerKey);
		Util.reqValidAddress(address);

		S.out( "%s minting %s %s", 
				Refblocks.getAddressPk(callerKey), amt, address);

		return Refblocks.exec( callerKey, tm -> load( tm, 100000)
				.mint( address, toBlockchain( amt) ) );
	}
}
