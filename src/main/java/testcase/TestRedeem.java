package testcase;

import org.json.simple.JsonObject;

import http.MyHttpClient;
import reflection.RefCode;
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
	
	// should fail
	public void testRedeem() throws Exception {
		Cookie.setNewWallet("0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce");
		JsonObject payload = new JsonObject();
		payload.put("cookie", Cookie.cookie);

		cli().post("/api/redemptions/redeem/" + Cookie.wallet, payload.toString() );
		S.out( "redeem: " + cli.readString() );
		assertEquals(200, cli.getResponseCode() );  // confirm that Cookie wallet has some RUSD in it
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
			.get("/api/redemptions/redeem/" + Cookie.wallet + "a");
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		
		// wrong address (must match cookie)
		String wallet = ("0xaaa" + Cookie.wallet).substring(0, 42);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + wallet);
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
	}
	
	public void testFailNoCookie() throws Exception {
		cli().get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "fail: " + cli.readString() );
		assertEquals(400, cli.getResponseCode() );
	}
}
