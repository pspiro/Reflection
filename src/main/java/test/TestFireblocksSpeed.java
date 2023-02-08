package test;

import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import tw.util.S;

public class TestFireblocksSpeed {
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 3; i++) {
			new Thread( () -> testone() ).start();
			S.sleep(100);
		}
	}
	
	static void testone() {
		try {
			testonea();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void testonea() throws Exception {
		Fireblocks.setTestVals();
//		deploy();
		//approveBusd();
		//String id = buyStock( userAddr, busdAddr, 10, StockToken.qqq, 11); // this works
		String id = Rusd.buyStock( Fireblocks.userAddr, Fireblocks.rusdAddr, 10, StockToken.qqq, 11); // this works
		String hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
		S.out( "%s got hash %s", id, hash);
		
		//buyStock( userAddr, busdAddr, 10, TestFireblocks.qqq, 11); // this works
		//buyStock( userAddr, rusdAddr, 10, StockToken.qqq, 11); // test this
//		buyRusd( userAddr, busdAddr, 9);   // this works, make sure you have called BUSD.approve(RUSD) for spending
		//mint( Rusd.userAddr, 2000);
	}
}
