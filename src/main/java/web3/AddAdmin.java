package web3;

import reflection.SingleChainConfig;

/** Add the Refblocks admin to the RUSD created with Fireblocks.
 *  Must be called by the Fireblocks owner.
 *  
 *  NOTE that this means you cannot have ownerKey in the Refblocks config because
 *  it will always be wrong */
public class AddAdmin {
	public static void main(String[] args) throws Exception {
		SingleChainConfig fb = SingleChainConfig.ask( "Dev");
		SingleChainConfig rb = SingleChainConfig.ask( "Dt2");
		
		fb.rusd().addOrRemoveAdmin("Owner", rb.admin1Addr(), true)
			.displayHash();
	}
}
