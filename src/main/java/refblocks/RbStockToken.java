package refblocks;

/** Implements the Busd contract methods that are writable, and deploy() */
public class RbStockToken {

	public static String deploy( String ownerKey, String name, String symbol, String rusdAddr) throws Exception {
		return Stocktoken.deploy( 
				Refblocks.web3j,
				Refblocks.getTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas),
				name, symbol, rusdAddr
				).send().getContractAddress();
	}

}
