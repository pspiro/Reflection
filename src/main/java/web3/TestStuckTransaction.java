package web3;

import chain.Chain;
import chain.Chains;
import tw.util.S;

public class TestStuckTransaction {
	public static void main(String[] args) throws Exception {
		Chain chain = new Chains().readOne( "Amoy", true);
		
		var addr = chain.params().admin1Addr();
		
		chain.blocks().showAllNonces( addr);

//		Refblocks.skip = true;
		chain.blocks().transfer( chain.params().admin1Key(), NodeInstance.prod, .0001)
			.waitForReceipt();
		;
		chain.node().showTrans( addr);

		chain.blocks().showAllNonces( addr);
		
	}
}
