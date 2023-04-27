package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;

public class TestWallet extends TestCase {
	static String host = "localhost"; //http://reflection.trading";

	public void test() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		String uri = String.format( "?msg=wallet&address=%s", Cookie.wallet);
		MyJsonObject obj = cli.get(uri).readMyJsonObject();
		obj.display();
		assertTrue( obj.getDouble("Native token balance") > 0);
	}
}
