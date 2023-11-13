package testcase;

import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import positions.Wallet;
import reflection.RefCode;
import reflection.Stocks;
import tw.util.S;

/** You must have some RUSD for these tests to pass */
public class TestRedeem extends MyTestCase {
	
	static String host = "localhost"; // "34.125.38.193";

	public static void mint(String wallet, double amt) throws Exception {
		S.out( "Minting %s RUSD into %s", amt, wallet);
		Stocks stocks = new Stocks();
		stocks.readFromSheet(m_config);

		m_config.rusd()
				.sellStockForRusd( wallet, amt, stocks.getAnyStockToken(), 0)
				.waitForStatus("COMPLETED");
	}

	public void testRedeem() throws Exception {
		// mint some RUSD into the wallet
		if (Wallet.getBalance(Cookie.wallet, m_config.rusdAddr() ) == 0) {
			mint(Cookie.wallet, 1.10009);  // the 9 should get truncated and we should end up with .00009 in the wallet
			waitForBalance(1, false);
		}

		// redeem RUSD
		cli().postToJson("/api/redeemRUSD/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
		assert200();

		// second one should fail w/ REDEMPTION_PENDING
		S.sleep(200);
		cli().postToJson("/api/redeemRUSD/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
		assertEquals( RefCode.REDEMPTION_PENDING, cli.getRefCode() );
		
		waitForRedeem(Cookie.wallet);
		waitForBalance(.0001, true);
	}
	
	/** Wait up to 10 sec for Moralis to catch up 
	 * @throws Exception */
	private void waitForBalance(double bal, boolean lt) throws Exception {
		for (int i = 0; i < 10; i++) {
			double balance = Wallet.getBalance(Cookie.wallet, m_config.rusdAddr() );
			if (lt && balance < bal || !lt && balance > bal) {
				return;
			}
			S.out( "balance: " + balance);
			S.sleep(1000);
		}
		assertTrue("Never achieved expected balance", false);
	}

	public void testCheckBalance() throws Exception {
		S.out( "Balance: " + Wallet.getBalance(Cookie.wallet, m_config.rusdAddr() ) );
	}
		
	
	private void waitForRedeem(String wallet) throws Exception {
		S.sleep(100); // give it time to get into the map
		while (true) {
			JsonObject rusd = cli().get("/api/mywallet/" + Cookie.wallet).readJsonObject().getArray("tokens").getJsonObj(0);
			String status = rusd.getString("status");
			if (S.isNotNull(status) ) {
				S.out( rusd.getString("text"));
				assertEquals( "Completed", status);
				break;
			}
			S.out( rusd.getInt("progress") );
			S.sleep(1000);
		}
	}

	public void testFailAddress() throws Exception {
		// invalid address (wrong length)
		cli().addHeader("Cookie", Cookie.cookie)
			.get("/api/redeemRUSD/" + Cookie.wallet + "a");
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		
		// wrong address (must match cookie)
		String wallet = ("0xaaa" + Cookie.wallet).substring(0, 42);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redeemRUSD/" + wallet);
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
	}
	
	public void testFailNoCookie() throws Exception {
		cli().get("/api/redeemRUSD/" + Cookie.wallet);
		S.out( "fail: " + cli.readString() );
		assertEquals(400, cli.getResponseCode() );
	}
}
