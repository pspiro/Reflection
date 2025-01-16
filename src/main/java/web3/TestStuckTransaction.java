package web3;

import chain.Chain;
import chain.Chains;
import common.Util;
import tw.util.S;

public class TestStuckTransaction {
	public static void main(String[] args) throws Exception {
		Chain chain = Chains.readOne( "Amoy", true);
		
		String key = Util.createPrivateKey();
		String wal = Util.getAddress(key);
		var tok = chain.getAnyStockToken();
		
		chain.showAdmin1Nonces();

		S.out( "minting 5 RUSD into " + wal);
		chain.rusd().mintRusd( wal, 5, tok).waitForReceipt();
		S.out( "position: " + chain.rusd().getPosition(wal) );
		
		Util.wrap( () -> {
			chain.rusd().transfer(wal, wal, 1000000)
				.waitForReceipt();
		});
		
		chain.showAdmin1Nonces();

		Util.wrap( () -> {
			chain.rusd().buyStockWithRusd( wal, 5, tok, 1) 
				.waitForReceipt();
		});
		
		chain.showAdmin1Nonces();
	}
}
