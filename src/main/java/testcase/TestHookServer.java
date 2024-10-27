package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.StockToken;

/** This test should be done in Dev or Prod only. Why?
 * 
 *  Requires only that HookServer be running */
public class TestHookServer extends MyTestCase {
	static String hook = "http://localhost:8080/hook";
	static String newWallet = Util.createFakeAddress();

	static {
		S.out( "testing with wallet %s", newWallet);

		// create the wallet first so we know we are getting values from the events
		try {
			MyClient.getJson( hook + "/get-wallet/" + newWallet);
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
		StockToken tok = stocks.getAnyStockToken();

		// mint RUSD
		S.out( "minting 10 rusd into %s", newWallet);
		m_config.rusd().mintRusd(newWallet, 10, stocks.getAnyStockToken() )
				.displayHash();

		S.out( "waiting for position from hookserver");
		waitFor( 60, () -> {
			JsonObject obj = MyClient.getJson( hook + "/get-wallet/" + newWallet)
					.getArray("positions")
					.find( "address", m_config.rusdAddr() );
			return obj != null ? obj.getDouble("position") == 10 : false; 
		});

		// buy stock token - wait for changes in RUSD and stock token
		S.out( "buying 1 stock token %s for %s", tok.address(), newWallet);
		m_config.rusd().buyStockWithRusd(newWallet, 1, tok, 2)
				.displayHash();

		S.out( "waiting for position");
		waitFor( 60, () -> {
			JsonObject obj2 = MyClient.getJson( hook + "/get-wallet/" + newWallet);
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
		double ethAmt = .00001;
		
		// send native token to wallet
		S.out( "testing native transfer");
		m_config.chain().blocks().transfer(
				m_config.ownerKey(), 
				newWallet,
				ethAmt).displayHash();

		// wait for it to appear
		waitFor( 60, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + newWallet)
					.getDouble( "native");
			S.out( String.format( "need=%s  hookserver=%s  query=%s",  // note that the query comes about 3 seconds quicker
					ethAmt, S.fmt4(pos), S.fmt4(node().getNativeBalance( newWallet) ) ) );
			return Util.isEq( pos, ethAmt, .000001);
		});
	}
	
	public void testApprove() throws Exception {
		int amt = Util.rnd.nextInt( 10000) + 10;

		// let Owner approve RUSD to spend BUSD
		S.out( "testing approve w/ random amt %s", amt);

		// create the owner wallet first so we know we get the event
		MyClient.getJson( hook + "/get-wallet/" + m_config.ownerAddr() );

		// let Owner approve RUSD to spend BUSD (no sig needed)
		m_config.busd().approve( m_config.ownerKey(), m_config.rusdAddr(), amt)
				.displayHash();
		
		// wait for it to be reflected in wallet
		waitFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + m_config.ownerAddr() )
					.getDouble( "approved");
			S.out( "need=%s  hookserver=%s  query=%s",  // note that the query comes about 3 seconds quicker
					amt, pos, m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() ) );
			return pos == amt; 
		});
	}
	
	/** test that hookserver is using correct decimals 
	 * @throws Exception */
	public void testBalances() throws Exception {
		var tok = stocks.getAnyStockToken();

		m_config.rusd().mintRusd( newWallet, 5, tok).waitForReceipt();
		m_config.rusd().mintStockToken(newWallet, tok, 6).waitForReceipt();
		if (!m_config.isProduction() ) {
			m_config.busd().mint( m_config.ownerKey(), newWallet, 7).waitForReceipt();
		}
		
		// wait for balances to appear in wallet locally
		waitForBalance(newWallet, m_config.rusdAddr(), 5, false);
		waitForBalance(newWallet, tok.address(), 6, false);
		if (!m_config.isProduction() ) {
			waitForBalance(newWallet, m_config.busdAddr(), 7, false);
		}
		
		var ret = MyClient.getJson( hook + "/get-wallet-map/" + newWallet);
		ret.display();

		// verify that hookserver has the balances as well
		var balances = ret.getObject( "positions");
		assertEquals( 5., balances.getDouble( m_config.rusdAddr() ) );
		assertEquals( 6., balances.getDouble( tok.address() ) );
		if (!m_config.isProduction() ) {
			assertEquals( 7., balances.getDouble( m_config.busdAddr() ) );
		}
	}
	
}
