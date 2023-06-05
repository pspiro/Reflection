package testcase;

import json.MyJsonObject;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends MyTestCase {
	public void test() throws Exception {
		cli().get("/api/mywallet/" + Cookie.wallet).readMyJsonObject().display();
		//double bal = cli.readMyJsonObject().getAr("tokens").find( "name", "USDC").getDouble("balance");
		//S.out(bal);
	}
}
