package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.NodeServer;
import web3.StockToken;

/** This test should be done in Dev or Prod only. Why?
 * 
 *  Requires only that HookServer be running */
public class TestHookServer extends MyTestCase {
	static String hook = "http://localhost:8484/hook";
	static String wallet = Util.createFakeAddress();

	static {
		S.out( "testing with wallet %s", wallet);

		// create the wallet first so we know we are getting values from the events
		try {
			MyClient.getJson( hook + "/get-wallet/" + wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				.displayHash();

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
				.displayHash();

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
		double n = .0001;
		
		// create the wallet first so we know we get the event
		MyClient.getJson( hook + "/get-wallet/" + wallet);

		// send native token to wallet
		S.out( "testing native transfer");
		m_config.matic().transfer(
				m_config.ownerKey(), 
				wallet,
				n).displayHash();

		// wait for it to appear
		waitFor( 60, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getDouble( "native");
			S.out( String.format( "need=%s  hookserver=%s  query=%s",  // note that the query comes about 3 seconds quicker
					n, pos, NodeServer.getNativeBalance( wallet) ) );
			return Util.isEq( pos, n, .00001);
		});
	}
	
	public void testApprove() throws Exception {
		int n = Util.rnd.nextInt( 10000) + 10;

		// let Owner approve RUSD to spend BUSD
		S.out( "testing approve");

		// create the wallet first so we know we get the event
		MyClient.getJson( hook + "/get-wallet/" + m_config.ownerAddr() );

		// let Owner approve RUSD to spend BUSD (no sig needed)
		m_config.busd().approve( m_config.ownerKey(), m_config.rusdAddr(), n)
				.displayHash();

		// wait for it to be reflected in wallet
		waitFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + m_config.ownerAddr() )
					.getDouble( "approved");
			S.out( "need=%s  hookserver=%s  query=%s",  // note that the query comes about 3 seconds quicker
					n, pos, m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() ) );
			return pos == n; 
		});
	}
	
	/** test that hookserver is using correct decimals 
	 * @throws Exception */
	public void testBalances() throws Exception {
		var tok = stocks.getAnyStockToken();
		String wallet = Util.createFakeAddress();
//		m_config.rusd().mintRusd( wallet, 5, tok).waitForHash();
//		m_config.busd().mint( m_config.ownerKey(), wallet, 6).waitForHash();
//		m_config.rusd().mintStockToken(wallet, tok, 7).waitForHash();

		m_config.rusd().mintRusd( wallet, 5, tok);
		m_config.busd().mint( m_config.ownerKey(), wallet, 6);
		m_config.rusd().mintStockToken(wallet, tok, 7);
		
		waitForBalance(wallet, m_config.rusdAddr(), 5, false);
		waitForBalance(wallet, m_config.busdAddr(), 6, false);
		waitForBalance(wallet, tok.address(), 7, false);
		
		var ret = MyClient.getJson( hook + "/get-wallet-map/" + wallet);
		ret.display();
		
		var balances = ret.getObject( "positions");
		
		assertEquals( 6, balances.getDouble( m_config.busdAddr() ) );
		assertEquals( 7, balances.getDouble( tok.address() ) );
		assertEquals( 5, balances.getDouble( m_config.rusdAddr() ) );
		
	}
	
}
