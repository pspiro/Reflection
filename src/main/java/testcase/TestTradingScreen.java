package testcase;

import tw.util.S;

public class TestTradingScreen extends MyTestCase {
	public void testDynamic() throws Exception {
		S.out( "trading-screen-dynamic");
		
		String url = String.format( "/api/trading-screen-dynamic/%s/265598", Cookie.wallet);
		var json = cli().postToJson( url, Cookie.getJson() );
		json.display();
		assert200();

		assertTrue( json.has( "askPrice", "bidPrice", "exchangeStatus", "exchangeTime", "nonRusdApprovedAmt", "nonRusdBalance" , "rusdBalance", "stockTokenBalance") );
		
		// zero conid
		url = String.format( "/api/trading-screen-dynamic/%s/0", Cookie.wallet);
		cli().postToJson( url, Cookie.getJson() );
		assert400();

		// invalid conid (no prices)
		url = String.format( "/api/trading-screen-dynamic/%s/1", Cookie.wallet);
		cli().postToJson( url, Cookie.getJson() );
		assert400();
	}

	public void testWatchList() throws Exception {
		S.out( "watch-list");
		
		var ar = cli().get( "/api/get-watch-list").readJsonArray();
		assert200();
		ar.display();
		
		var item = ar.get( 0);
		item.display();

		assertTrue( item.has( "bid", "ask", "symbol", "conid") );
	}
}
