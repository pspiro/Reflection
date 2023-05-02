package testcase;

import http.MyHttpClient;
import reflection.RefCode;
import tw.util.S;

public class TestRedeem extends MyTestCase {
	
	static String host = "localhost"; // "34.125.38.193";
	
	public void testFailAddress() throws Exception {
		// invalid address (wrong length)
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet + "a");
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getCode() );
		
		// wrong address (must match cookie)
		String wallet = ("0xaaa" + Cookie.wallet).substring(0, 42);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + wallet);
		S.out( "failAddress: " + cli.getMessage() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getCode() );
		
		// wrong nonce
	}
	
	public void testRedeem() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "redeem: " + cli.readString() );
		assertEquals(200, cli.getResponseCode() );  // confirm that Cookie wallet has some RUSD in it
	}
	
	public void testFailNoCookie() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli.get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "fail: " + cli.readString() );
		assertEquals(400, cli.getResponseCode() );  // confirm that Cookie wallet has some RUSD in it
	}
}
