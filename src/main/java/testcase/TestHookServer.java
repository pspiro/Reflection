package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import chain.Chains;
import common.Util;
import http.MyClient;
import tw.util.S;
import web3.StockToken;

/** This test should be done in Dev or Prod only. Why?
 * 
 *  Requires only that HookServer be running */
public class TestHookServer extends MyTestCase {
	static String hook = "http://localhost:6001/hook";
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
		StockToken tok = chain.getAnyStockToken();

		// mint RUSD
		S.out( "minting 10 rusd into %s", newWallet);
		chain.rusd().mintRusd(newWallet, 10, chain.getAnyStockToken() )
				.displayHash();

		S.out( "waiting for position from hookserver");
		waitFor( 60, () -> {
			JsonObject obj = MyClient.getJson( hook + "/get-wallet/" + newWallet)
					.getArray("positions")
					.find( "address", chain.params().rusdAddr() );
			return obj != null ? obj.getDouble("position") == 10 : false; 
		});

		// buy stock token - wait for changes in RUSD and stock token
		S.out( "buying 1 stock token %s for %s", tok.address(), newWallet);
		chain.rusd().buyStockWithRusd(newWallet, 1, tok, 2)
				.displayHash();

		S.out( "waiting for position");
		waitFor( 60, () -> {
			JsonObject obj2 = MyClient.getJson( hook + "/get-wallet/" + newWallet);
			JsonArray ar = obj2.getArray("positions");
			JsonObject rusd = ar.find( "address", chain.params().rusdAddr() );
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
		chain.blocks().transfer(
				chain.params().ownerKey(), 
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
		MyClient.getJson( hook + "/get-wallet/" + chain.params().ownerAddr() );

		// let Owner approve RUSD to spend BUSD (no sig needed)
		chain.busd().approve( chain.params().ownerKey(), chain.params().rusdAddr(), amt)
				.displayHash();
		
		// wait for it to be reflected in wallet
		waitFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + chain.params().ownerAddr() )
					.getDouble( "approved");
			S.out( "need=%s  hookserver=%s  query=%s",  // note that the query comes about 3 seconds quicker
					amt, pos, chain.busd().getAllowance( chain.params().ownerAddr(), chain.params().rusdAddr() ) );
			return pos == amt; 
		});
	}
	
	/** test that hookserver is using correct decimals 
	 * @throws Exception */
	public void testBalances() throws Exception {
		var tok = chain.getAnyStockToken();

		chain.rusd().mintRusd( newWallet, 5, tok).waitForReceipt();
		chain.rusd().mintStockToken(newWallet, tok, 6).waitForReceipt();
		if (!chain.params().isProduction() ) {
			chain.busd().mint( chain.params().ownerKey(), newWallet, 7).waitForReceipt();
		}
		
		// wait for balances to appear in wallet locally
		waitForBalance(newWallet, chain.params().rusdAddr(), 5, false);
		waitForBalance(newWallet, tok.address(), 6, false);
		if (!chain.params().isProduction() ) {
			waitForBalance(newWallet, chain.params().busdAddr(), 7, false);
		}
		
		var ret = MyClient.getJson( hook + "/get-wallet-map/" + newWallet);
		ret.display();

		// verify that hookserver has the balances as well
		var balances = ret.getObject( "positions");
		assertEquals( 5., balances.getDouble( chain.params().rusdAddr() ) );
		assertEquals( 6., balances.getDouble( tok.address() ) );
		if (!chain.params().isProduction() ) {
			assertEquals( 7., balances.getDouble( chain.params().busdAddr() ) );
		}
	}
	
}
