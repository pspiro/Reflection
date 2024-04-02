package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import test.Cookie;
import tw.util.S;

public class TestBackendMsgs extends MyTestCase {
//	server.createContext("/api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
//	server.createContext("/api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );

	String host = "localhost";
	
	public void testHotStocks() throws Exception {
		cli().get("/api/hot-stocks");
		assertTrue( cli.readJsonArray().size() > 0);
	}

	public void testGetAllStocks() throws Exception {
		cli().get("/api/get-all-stocks");
		JsonArray ar = cli.readJsonArray();
		JsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
		assertTrue(item.getDouble("bid") > 0);
		assertTrue(item.getDouble("ask") > 0);
	}
	
	public void testGetStocksWithPrices() throws Exception {
		cli().get("/api/get-stocks-with-prices");
		JsonArray ar = cli.readJsonArray();
		assertTrue( ar.size() > 0);
		JsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
	}
	
	public void testGetStockWithPrice() throws Exception {
		cli().get("/api/get-stock-with-price/265598");
		JsonObject obj = cli.readJsonObject();
		assert200();
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		
		startsWith( "AAPL", obj.getString("symbol") );
		assertEquals( "Stock", obj.getString("type") );
		assertEquals( "265598", obj.getString("conid") );
		assertEquals( "open", obj.getString("exchangeStatus") );
		assertEquals( "NASDAQ:AAPL", obj.getString("tradingView") );
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetPrice() throws Exception {
		cli().get("/api/get-price/265598");
		JsonObject obj = cli.readJsonObject();
		assert200();
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetCryptos() throws Exception {
		cli().get("/api/crypto-transactions");
		JsonArray ar = cli.readJsonArray();
		S.out( "all crypto");
		S.out( ar.getJsonObj(0) );
		assert200();
		assertTrue( ar.size() > 1);
	}
	
	public void testGetCryptosByAddr() throws Exception {
		cli().get("/api/crypto-transactions/?wallet_public_key=" + Cookie.wallet);
		JsonArray ar = cli.readJsonArray();
		assert200();
		assertTrue( ar.size() > 1);
	}
	
	public void testOnramp() throws Exception {
		cli().postToJson( "http://localhost:8383/api/onramp", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"transactionId", "abc").toString() ).display();
		assert200();
	}
}
