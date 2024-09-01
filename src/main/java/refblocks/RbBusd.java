package refblocks;

import java.util.function.Supplier;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import common.Util;
import common.Util.ExSupplier;
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
	public Busd load(String callerKey, int gas) throws Exception {
		return Busd.load( 
				address(), 
				Refblocks.web3j, 
				Refblocks.getFasterTm( callerKey), 
				Refblocks.getGp( gas)  // sends a query
				);
	}

	/** Used during deployment and whenever RefWallet or Busd changes */ 
	@Override public RetVal approve(String approverKey, String spenderAddr, double amt) throws Exception {
		Util.reqValidKey(approverKey);
		Util.reqValidAddress(spenderAddr);
		
		S.out( "%s approving %s to spend %s %s", 
				Refblocks.getAddressPk(approverKey), spenderAddr, amt, m_name);
		
		var contract = load( approverKey, 100000);
		return contract.exec( () -> contract.approve( spenderAddr, toBlockchain( amt) ) );
	}
		
	/** For testing only; anyone can call this but they must have some gas */
	@Override public RetVal mint( String callerKey, String address, double amt) throws Exception {
		Util.reqValidKey(callerKey);
		Util.reqValidAddress(address);

		S.out( "%s minting %s %s for %s", 
				Refblocks.getAddressPk(callerKey), amt, m_name, address);

		var contract = load( callerKey, 100000);
		return contract.exec( () -> contract.mint( address, toBlockchain( amt) ) );
	}

	
	/** transfer ERC-20 token */
	@Override public RetVal transfer(String fromKey, String toAddr, double amt) throws Exception {
		Util.reqValidKey(fromKey);
		Util.reqValidAddress(toAddr);
		
		S.out( "transferring %s %s from %s to %s",
				amt, m_name, Refblocks.getAddressPk( fromKey), toAddr);

		var contract = load( fromKey, 100000);
		return contract.exec( () -> contract.transfer( toAddr, toBlockchain( amt) ) );
	}
}
