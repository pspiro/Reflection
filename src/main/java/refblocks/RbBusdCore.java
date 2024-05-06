package refblocks;

import fireblocks.RetVal;
import web3.Busd.IBusd;
import web3.MyCoreBase;

public class RbBusdCore extends MyCoreBase implements IBusd {
	public RbBusdCore( String address, int decimals, String name) {
		super( address, decimals, name);
	}

	public static String deploy(String ownerKey) throws Exception {
		return Busd.deploy( 
				Refblocks.web3j,
				Refblocks.getTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas)
				).send().getContractAddress();
	}

	/** For testing only */
	@Override public RetVal approve(String spenderAddr, double amt) throws Exception {
		return null;
	}

	@Override public RetVal mint(String address, double amount) {
		return null;
	}

}
