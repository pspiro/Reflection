package testcase;

import static testcase.TestErrors.sendData;

import java.sql.ResultSet;

import org.json.simple.JSONObject;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.MySqlConnection;
import tw.util.S;

public class TestConfig extends TestCase {
	
	public void testConnection() throws Exception {
		String data = "{ 'msg': 'getconnectionstatus' }"; 
		MyJsonObject map = sendData( data);
		assertEquals( "true", map.get("orderConnectedToTWS") );
		assertEquals( "true", map.get("orderConnectedToBroker") );		
	}
	
	public void testRefAPIConfig() throws Exception {
		String data = "{ 'msg': 'getconfig' }"; 
		MyJsonObject map = sendData( data);
		assertEquals( "18", map.get( "busdDecimals") );
	}

	public void testRefreshConfig() throws Exception {
		String data = "{ 'msg': 'refreshconfig' }"; 
		MyJsonObject map = sendData( data);
		assertEquals( "OK", map.get( "code") );
	}

	public void testBackendConfigs() throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		MyJsonArray ar = cli.get("/api/faqs").readMyJsonArray();
		S.out( ar.getJsonObj(0) );
		assertTrue( ar.size() > 3);

		cli = new MyHttpClient("localhost", 8383);
		JSONObject obj = cli.get("/api/system-configurations/last").readJsonObject();
		S.out( obj.get("min_order_size"));
		assertTrue( obj.size() > 3);
		
		cli = new MyHttpClient("localhost", 8383);
		obj = cli.get("/api/configurations").readJsonObject();
		assertTrue( obj.size() > 3);
		
		cli = new MyHttpClient("localhost", 8383);
		obj = cli.get("/api/configurations?key=whitepaper_text").readJsonObject();
		S.out( obj);
		assertEquals(1, obj.size() );
		
		
		
	}
}
