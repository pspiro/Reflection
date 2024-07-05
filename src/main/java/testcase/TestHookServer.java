package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.StockToken;

/** This test should be done in Dev or Prod only */
public class TestHookServer extends MyTestCase {
	//String hook = "https://live.reflection.trading/hook";
	String hook = "http://localhost:8484/hook";
	static String wallet = Util.createFakeAddress();

	static {
		S.out( "testing with wallet %s", wallet);
	}
	
	void runn() throws Exception {
		testTransfers();
		testNative();
		testApprove();
	}

	public void testShow() throws Exception {
		MyClient.getJson( hook + "/get-wallet/0xdA2c28Af9CbfaD9956333Aba0Fc3B482bc0AeD13")
				.display();
	}
		
	public void testTransfers() throws Exception {
//		// let the HookServer start monitoring for this wallet
		MyClient.getJson( hook + "/get-wallet/" + wallet);
		
		StockToken tok = stocks.getAnyStockToken();

		// mint RUSD
		S.out( "minting 10 rusd into %s", wallet);
		m_config.rusd().mintRusd(wallet, 10, stocks.getAnyStockToken() )
				.waitForHash();

		S.out( "waiting for position");
		waitFor( 60, () -> {
			JsonObject obj = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getArray("positions")
					.find( "address", m_config.rusdAddr() );
			return obj != null ? obj.getDouble("position") == 10 : false; 
		});

		// buy stock token - wait for changes in RUSD and stock token
		S.out( "buying 1 stock token %s for %s", tok.address(), wallet);
		m_config.rusd().buyStockWithRusd(wallet, 1, tok, 2)
				.waitForHash();

		S.out( "waiting for position");
		waitFor( 60, () -> {
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
		m_config.matic().transfer(
				m_config.ownerKey(), 
				wallet,
				.001).waitForHash();
		
		// wait for it to appear
		waitFor( 60, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getDouble( "native");
			S.out( "pos " + pos);
			return Util.isEq( pos, .001, .00001);
		});
	}
	
	public void testApprove() throws Exception {
		int n = Util.rnd.nextInt( 10000) + 10;
		
		// let Owner approve RUSD to spend BUSD
		m_config.busd().approve( m_config.ownerKey(), m_config.rusdAddr(), n)
				.waitForHash();

		// wait for it to be reflected in wallet
		waitFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + m_config.ownerAddr() )
					.getDouble( "approved");
			S.out( m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() ) );
			return pos == n; 
		});
	}
	
}
