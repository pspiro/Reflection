package test;

import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import tw.util.S;

public class TestFireblocksSpeed {
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 1; i++) {
			new Thread( () -> testone() ).start();
			S.sleep(500);
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
		Fireblocks.setProdValsPolygon();
//		deploy();
		//approveBusd();
		
		
		//String id = buyStock( userAddr, busdAddr, 10, StockToken.qqq, 11); // this works
		String id = Rusd.buyStock( myWallet, Fireblocks.rusdAddr, 20000, StockToken.ge, 11, "test speed"); // this works
		String hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
		S.out( "%s got hash %s", id, hash);
		
		//buyStock( userAddr, busdAddr, 10, TestFireblocks.qqq, 11); // this works
		//buyStock( userAddr, rusdAddr, 10, StockToken.qqq, 11); // test this
//		buyRusd( userAddr, busdAddr, 9);   // this works, make sure you have called BUSD.approve(RUSD) for spending
		//mint( Rusd.userAddr, 2000);
	}
	
	/* Test results
	 * three orders, 500 ms apart
	 * start: 17:05:25.266
	 * end:   17:06:16.894
	 * 45 seconds
	 */
}


/* test from Remix on polygon
0xb016711702D3302ceF6cEb62419abBeF5c44450e
0x3a59c94c3b80631ed883384c5d3c2db06c66a151
0x17211791ea7529a18f18f9247474338a5aee226b
10000000
11000000000000000000
*/