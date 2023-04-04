package testcase;

import http.MyHttpClient;
import junit.framework.TestCase;
import tw.util.S;

public class TestRedeem extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	static String prod = "34.125.38.193";
	
	public void test1() throws Exception {
		MyHttpClient cli = new MyHttpClient(prod, 8383);
		cli.get("api/redemptions/redeem/" + wallet);
		S.out( cli.readString() );
	}
}
