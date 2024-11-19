package test;

import chain.Chains;

public class TransferNative {
	static String ownerKey = "";
	public static void main(String[] args) throws Exception {
		approve();
	}
	
	static void transfer() throws Exception {
		var chain = new Chains().readOne("PulseChain", false);
		chain.blocks().transfer( 
				ownerKey, 
				"0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce", 
				1).waitForReceipt();
	}
	
	public static void approve() throws Exception {
		var chain = new Chains().readOne("PulseChain", false);
		chain.busd().approve( ownerKey, chain.rusd().address(), 200)
			.waitForReceipt();
		
	}

}
