package testcase;

import tw.util.S;

public class TestTradingScreen extends MyTestCase {
	public void testStatic() throws Exception {
		S.out( "trading-screen-static");

		String url = String.format( "/api/trading-screen-static/%s/265598", Cookie.wallet);
		var json = cli().postToJson( url, Cookie.getJson() );
		json.display();
		assert200();
		
		assertTrue( json.has( "symbol", "tokenSymbol", "tradingView", "description", "conid", "smartContractid") );
	}
	
	public void testDynamic() throws Exception {
		S.out( "trading-screen-dynamic");
		
		String url = String.format( "/api/trading-screen-dynamic/%s/265598", Cookie.wallet);
		var json = cli().postToJson( url, Cookie.getJson() );
		json.display();
		assert200();

		assertTrue( json.has( "askPrice", "bidPrice", "exchangeStatus", "exchangeTime", "nonRusdApprovedAmt", "nonRusdBalance" , "rusdBalance", "stockTokenBalance") );
	}

	public void testWatchList() throws Exception {
		S.out( "watch-list");
		
		var ar = cli().get( "/api/get-watch-list").readJsonArray();
		assert200();
		
		var item = ar.get( 0);
		item.display();

		assertTrue( item.has( "bid", "ask", "symbol", "conid") );
	}
}
