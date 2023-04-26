package testcase;

import http.MyHttpClient;
import junit.framework.TestCase;
import tw.util.S;

public class TestRedeem extends TestCase {
	
	static String host = "localhost"; // "34.125.38.193";
	
	public void testRedeem() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		cli	.addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "redeem: " + cli.readString() );
		assertEquals(200, cli.getResponseCode() );  // confirm that Cookie wallet has some RUSD in it
	}
}
