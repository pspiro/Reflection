package testcase;

import static testcase.TestErrors.sendData;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Config;
import tw.util.S;

public class TestConfig extends TestCase {
	//static String host = "34.125.38.193"; //http://reflection.trading";
	static String host = "localhost"; //http://reflection.trading";
	
	public void testConnection() throws Exception {
		String data = "{ 'msg': 'getconnectionstatus' }"; 
		JsonObject map = sendData( data);
		assertEquals( "true", map.getString("orderConnectedToTWS") );
		assertEquals( "true", map.getString("orderConnectedToBroker") );		
	}
	
	public void testRefAPIConfig() throws Exception {
		String data = "{ 'msg': 'getconfig' }"; 
		JsonObject map = sendData( data);
		assertEquals( "18", map.getString( "busdDecimals") );
	}

	public void testRefreshConfig() throws Exception {
		String data = "{ 'msg': 'refreshconfig' }"; 
		JsonObject map = sendData( data);
		assertEquals( "OK", map.getString( "code") );
	}

	public void testBackendConfigs() throws Exception {
		MyHttpClient cli = new MyHttpClient(host, 8383);
		JsonArray ar = cli.get("/api/faqs").readJsonArray();
		S.out( ar.getJsonObj(0) );
		assertTrue( ar.size() > 3);

		cli = new MyHttpClient(host, 8383);
		JsonObject obj = cli.get("/api/system-configurations/last").readJsonObject();
		S.out( obj.get("min_order_size"));
		assertTrue( obj.size() > 5);
		
		cli = new MyHttpClient(host, 8383);
		obj = cli.get("/api/configurations").readJsonObject();
		S.out( obj);
		assertTrue( obj.size() > 5);
		
		cli = new MyHttpClient(host, 8383);
		obj = cli.get("/api/configurations?key=whitepaper_text").readJsonObject();
		S.out( obj);
		assertEquals(1, obj.size() );
	}
	
	public void testSendEmail() throws Exception {
		Config.readFrom("Dt-config").sendEmail("peteraspiro@gmail.com", "abc", "def", false);		
	}
	
	
	
}
