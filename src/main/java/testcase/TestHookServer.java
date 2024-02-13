package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExSupplier;
import fireblocks.Accounts;
import fireblocks.Fireblocks;
import fireblocks.StockToken;
import http.MyClient;
import positions.Streams;
import tw.util.S;

/** This test should be done in Dev or Prod only */
public class TestHookServer extends MyTestCase {
	//String hook = "https://live.reflection.trading/hook";
	String hook = "http://localhost:8080/hook";
	static String wallet = Util.createFakeAddress();

	static {
		readStocks();
		S.out( "testing with wallet %s", wallet);
	}
	
	public static void main(String[] args) throws Exception {
		new TestHookServer().runn();
		S.out( "all done");
	}
	
	void runn() throws Exception {
		testTransfers();
		testNative();
		testApprove();
	}

	public void testShow() throws Exception {
		String ownerWal = Accounts.instance.getAddress("Owner");
		S.out( ownerWal);
		MyClient.getJson( hook + "/get-wallet/0xdA2c28Af9CbfaD9956333Aba0Fc3B482bc0AeD13").display();
	}
		
	public void testTransfers() throws Exception {
//		// let the HookServer start monitoring for this wallet
//		MyClient.getJson( hook + "/get-wallet/" + wallet);
		
		StockToken tok = stocks.getAnyStockToken();

		// mint RUSD
		m_config.rusd().mintRusd(wallet, 10, stocks.getAnyStockToken() ).waitForHash();

		tryFor( 60, () -> {
			JsonObject obj = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getArray("positions")
					.find( "address", m_config.rusdAddr() );
			return obj != null ? obj.getDouble("position") == 10 : false; 
		});

		
		// buy stock token - wait for changes in RUSD and stock token
		m_config.rusd().buyStockWithRusd(wallet, 1, tok, 2);
		
		tryFor( 60, () -> {
			JsonObject obj2 = MyClient.getJson( hook + "/get-wallet/" + wallet);
			JsonArray ar = obj2.getArray("positions");
			JsonObject rusd = ar.find( "address", m_config.rusdAddr() );
			JsonObject stk = ar.find( "address", tok.address() );
			return getPos( rusd) == 9 && getPos( stk) == 2;
		});
	}
	
	private static double getPos(JsonObject pos) {
		return pos != null ? pos.getDouble("position") : 0;
	}

	public void testNative() throws Exception {
		// send native token to wallet
		Fireblocks.transfer(
				Accounts.instance.getId("Owner"), 
				wallet,
				Fireblocks.platformBase,
				.001, "test").waitForHash();
		
		// wait for it to appear
		tryFor( 60, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getDouble( "native");
			return Util.isEq( pos, .001, .00001);
		});
	}
	
	public void testApprove() throws Exception {
		String ownerWal = Accounts.instance.getAddress("Owner");
		int n = Util.rnd.nextInt( 10000) + 1;

		// let Owner approve RUSD to spend BUSD
		m_config.busd().approve(Accounts.instance.getId("Owner"), m_config.rusdAddr(), n)
				.waitForHash();

		// wait for it to be reflected in wallet
		tryFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + ownerWal)
					.getDouble( "approved");
			return pos == n;
		});
	}
	

	/** wait n seconds for supplier to return true, then fail */
	static void tryFor( int sec, ExSupplier<Boolean> sup) throws Exception {
		for (int i = 0; i < sec; i++) {
			S.out( i);
			if (sup.get() ) {
				S.out( "succeeded in %s seconds", i);
				return;
			}
			S.sleep(1000);
		}
		assertTrue( false);
	}
}
