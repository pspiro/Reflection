package test;

import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import reflection.Util;
import tw.util.S;

public class TestFireblocksSpeed {
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	static String myStock = "0x0b55eeb4a4d9a709b1144b6991c463e9ff10648d"; // deployed with RUSD w/ two RefWallets

	public static void main(String[] args) throws Exception {
		Fireblocks.setProdValsPolygon();
		
		// Util.execute( () -> testonea( Fireblocks.refWalletAcctId1) );  // fails, id=a8d998bf-f908-4328-9a0c-9036428c707e
		
		Util.execute( () -> testonea( Fireblocks.refWalletAcctId1) );  // fails, id=a8d998bf-f908-4328-9a0c-9036428c707e
//		Util.execute( () -> testonea( Fireblocks.refWalletAcctId2) );
	}

	static void testonea(int acctId) {
		try {
			String id = Rusd.buyStock( acctId, null, myWallet, Fireblocks.busdAddr, 1, myStock, 11, "test speed"); // this works
			String hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
			S.out( "%s got hash %s", id, hash);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
}
