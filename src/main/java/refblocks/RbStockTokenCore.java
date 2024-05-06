package refblocks;

public class RbStockTokenCore {

	public static String deploy( String ownerKey, String name, String symbol, String rusdAddr) throws Exception {
		return Stocktoken.deploy( 
				Refblocks.web3j,
				Refblocks.getTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas),
				name, symbol, rusdAddr
				).send().getContractAddress();
	}

}
