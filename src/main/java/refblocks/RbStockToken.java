package refblocks;

import java.math.BigInteger;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import common.Util;
import tw.util.S;

/** Implements the contract methods that are writable, and deploy() */
public class RbStockToken {

	public static String deploy( String ownerKey, String name, String symbol, String rusdAddr) throws Exception {
		return Stocktoken.deploy( 
				Refblocks.web3j,
				Refblocks.getWaitingTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas),
				name, symbol, rusdAddr
				).send().getContractAddress();
	}

	Stocktoken load(String privateKey, String address) throws Exception {
		return Stocktoken.load( 
				address, 
				Refblocks.web3j, 
				Refblocks.getWaitingTm(privateKey), 
				Refblocks.getGp(100000)
				);
	}
	
	/** Never called by us in real life; you can try calling it to see that it fails */
	void mint(String privateKey, String address ) throws Exception {
		TransactionReceipt receipt = load( privateKey, address).mint( 
				Util.createFakeAddress(), 
				BigInteger.valueOf( 1)
				).send();
		
		S.out( Refblocks.toString( receipt) );
	}
}
