package fireblocks;

import web3.Matic;

public class FbMatic extends Matic {

	@Override public void send(String privateKey, String to, double amt) {
		Fireblocks.transfer( privateKey, to, amt);
	}

}
