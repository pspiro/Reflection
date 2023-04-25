package testcase;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;

import org.json.simple.JSONArray;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import tw.util.S;

public class TestBackendMsgs extends TestCase {
//	server.createContext("/api/reflection-api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
//	server.createContext("/api/reflection-api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );

	String host = "localhost";
	
	public void testGetAllStocks() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/reflection-api/get-all-stocks");
		MyJsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);
		MyJsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
	}
	
	public void testGetStocksWithPrices() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/reflection-api/get-stocks-with-prices");
		MyJsonArray ar = cli.readMyJsonArray();
		assertTrue( ar.size() > 0);
		MyJsonObject item = ar.getJsonObj(0);
		assertNotNull(item.getString("symbol"));
		assertNotNull(item.getString("type"));
		assertNotNull(item.getString("conid"));
	}
	
	public void testGetStockWithPrice() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/reflection-api/get-stock-with-price/8314");
		MyJsonObject obj = cli.readMyJsonObject();
		assertEquals( 200, cli.getResponseCode() );
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		
		assertEquals( "IBM", obj.getString("symbol") );
		assertEquals( "Stock", obj.getString("type") );
		assertEquals( "8314", obj.getString("conid") );
		assertEquals( "open", obj.getString("exchangeStatus") );
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetPrice() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/reflection-api/get-price/8314");
		MyJsonObject obj = cli.readMyJsonObject();
		assertEquals( 200, cli.getResponseCode() );
		
		double bid = Double.valueOf( obj.getString("bid") );		
		double ask = Double.valueOf( obj.getString("ask") );		
		assertTrue( bid > 100 && bid < 200);
		assertTrue( ask > 100 && bid < 200);
	}
	
	public void testGetCryptos() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/crypto-transactions");
		MyJsonArray ar = cli.readMyJsonArray();
		S.out( ar);
		assertEquals( 200, cli.getResponseCode() );
		assertTrue( ar.size() > 1);
	}
	
	public void testGetCryptosByAddr() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/api/crypto-transactions/?wallet_public_key=" + Cookie.wallet);
		MyJsonArray ar = cli.readMyJsonArray();
		S.out( ar);
		assertEquals( 200, cli.getResponseCode() );
		assertTrue( ar.size() > 1);
	}
	
	public void test() throws SQLException, Exception {
		String where = String.format( "where lower(wallet_public_key)='%s'", Cookie.wallet.toLowerCase());
		JSONArray json = Config.readFrom("Dt-config")
				.sqlConnection()
				.queryToJson("select * from crypto_transactions %s order by created_at", where);
		json.forEach( obj -> fix((HashMap)obj) ); 
		S.out( json);
	}
	
	/** Convert timestamps from Timestamp to integer */
	static String tag = "created_at";
	private void fix(HashMap obj) {
		Timestamp ts = (Timestamp)obj.get(tag);  //<<<move this into RefAPI
		if (ts != null) {
			obj.put(tag, ts.getTime() / 1000); 
		}
	}
}
