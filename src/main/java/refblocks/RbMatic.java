package refblocks;

import web3.Matic;

public class RbMatic extends Matic {

	@Override public void send(String privateKey, String to, double amt) throws Exception {
		Refblocks.transfer( privateKey, to, amt);
	}

}
