package testcase;

import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Config;
import tw.util.S;

public class TestRedeem extends TestCase {
	
	static String host = "localhost"; // "34.125.38.193";
	
	public void testFailAddress() throws Exception {
		Config config = Config.readFrom("Dt-config");

		// invalid address (wrong length)
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet + "a");
		S.out( "failAddress: " + cli.readJsonObject().get("message") );
		assertEquals( 400, cli.getResponseCode() );
		
		// wrong address (must match cookie)
		String wallet = ("0xaaa" + Cookie.wallet).substring(0, 42);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + wallet);
		S.out( "failAddress: " + cli.readJsonObject().get("message") );
		assertEquals( 400, cli.getResponseCode() );
	}
	
	public void testRedeem() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "redeem: " + cli.readString() );
		assertEquals(200, cli.getResponseCode() );  // confirm that Cookie wallet has some RUSD in it
	}
}
