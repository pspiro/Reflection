package refblocks;

import common.Util;
import reflection.Config;
import tw.util.S;
import web3.NodeInstance;

/** This actually works as of 8/19/24 on pulsechain */
public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		Config c = Config.ask();
		
		String wallet = c.admin1Addr();  // wallet that is stuck
		
		// show stuck transactions
		c.node().showTrans( wallet);
				
		Util.pause();

		int nonce = 0x383;  // the nonce of the stuck transaction; you can auto-pull it from the stuck transaction json

		Refblocks.cancelStuckTransaction( c.admin1Key(), nonce);

		S.out( c.ownerAddr() );
		
	}
}
