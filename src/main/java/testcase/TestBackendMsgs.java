package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import http.MyHttpClient;
import tw.util.S;

public class TestBackendMsgs extends MyTestCase {
//	server.createContext("/api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
//	server.createContext("/api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );

	String host = "localhost";
	
	public void testGetAllStocks() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/get-all-stocks");
		JsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);
		JsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
	}
	
	public void testGetStocksWithPrices() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/get-stocks-with-prices");
		JsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);
		JsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
	}
	
	public void testGetStockWithPrice() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/get-stock-with-price/265598");
		JsonObject obj = cli.readMyJsonObject();
		assertEquals( 200, cli.getResponseCode() );
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		
		startsWith( "AAPL", obj.getString("symbol") );
		assertEquals( "Stock", obj.getString("type") );
		assertEquals( "265598", obj.getString("conid") );
		assertEquals( "open", obj.getString("exchangeStatus") );
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetPrice() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/get-price/265598");
		JsonObject obj = cli.readMyJsonObject();
		assertEquals( 200, cli.getResponseCode() );
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetCryptos() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/crypto-transactions");
		JsonArray ar = cli.readMyJsonArray();
		S.out( "all crypto");
		S.out( ar.getJsonObj(0) );
		assertEquals( 200, cli.getResponseCode() );
		assertTrue( ar.size() > 1);
	}
	
	public void testGetCryptosByAddr() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/crypto-transactions/?wallet_public_key=" + Cookie.wallet);
		JsonArray ar = cli.readMyJsonArray();
		S.out( "crypto by addr");
		S.out( ar.getJsonObj(0));
		assertEquals( 200, cli.getResponseCode() );
		assertTrue( ar.size() > 1);
	}
}
