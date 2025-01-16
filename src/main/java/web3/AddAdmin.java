package web3;

import chain.Chain;
import chain.Chains;

/** Add the Refblocks admin to the RUSD created with Fireblocks.
 *  Must be called by the Fireblocks owner.
 *  
 *  NOTE that this means you cannot have ownerKey in the Refblocks config because
 *  it will always be wrong */
public class AddAdmin {
	public static void main(String[] args) throws Exception {
		addAdmin( "Polygon");
	}

	public static void addAdmin( String chain) throws Exception {
		Chain fb = Chains.readOne( chain, false);
		
		fb.rusd().addOrRemoveAdmin(fb.params().ownerKey(), fb.params().sysAdminAddr(), true)
			.waitForReceipt();
	}
}
