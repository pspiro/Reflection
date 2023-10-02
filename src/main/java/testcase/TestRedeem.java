package testcase;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import http.MyHttpClient;
import reflection.RefCode;
import reflection.Stocks;
import tw.util.S;

/** You must have some RUSD for these tests to pass */
public class TestRedeem extends MyTestCase {
	
	static String host = "localhost"; // "34.125.38.193";

	public void testMyAfter() throws Exception {
		MyHttpClient cli = cli();
		JsonObject obj = cli.get("/api/mywallet/" + Cookie.wallet).readJsonObject();
		obj.display("My Wallet");
		
		assertTrue( obj.getInt("refresh") > 100);
		
		JsonObject tok = obj.getArray("tokens").getJsonObj(0);
		startsWith("RUSD", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
	}

	public static void mint(String wallet, double amt) throws Exception {
		S.out( "Minting %s RUSD into %s", amt, wallet);
		Stocks stocks = new Stocks();
		stocks.readFromSheet(m_config);

		m_config.rusd()
				.sellStockForRusd( wallet, amt, stocks.getAnyStockToken(), 0)
				.waitForStatus("COMPLETED");
	}
	
	public void testRedeem() throws Exception {
		// make sure we have at least some RUSD
		mint(Cookie.wallet, 1.00009);  // the 9 should get truncated and we should end up with .00009 in the wallet
		
		JsonObject payload = new JsonObject();
		payload.put("cookie", Cookie.cookie);

		cli().post("/api/redeemRUSD/" + Cookie.wallet, payload.toString() );
		assert200();     // confirm that Cookie wallet has some RUSD in it

		// second time should fail
		cli().post("/api/redeemRUSD/" + Cookie.wallet, payload.toString() );
		assertEquals( 400, cli.getResponseCode() );
	}
	
	public void testMyBefore() throws Exception {
		JsonObject obj = cli().get("/api/mywallet/" + Cookie.wallet).readJsonObject();
		obj.display("My Wallet");
		
		assertTrue( obj.getInt("refresh") > 100);
		
		JsonObject tok = obj.getArray("tokens").getJsonObj(0);
		startsWith("RUSD", tok.getString("name"));
		assertTrue( tok.getDouble("balance") > 0 );
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
	
	public static void main(String[] args) throws Exception {
		String walletAddr = Cookie.wallet;
		
		Rusd rusd = m_config.rusd();

		double rusdPos = Util.truncate( rusd.getPosition(walletAddr), 4); // truncate after four digits because Erc20 rounds to four digits when converting to Blockchain mode
		S.out( rusdPos);
	}
}
