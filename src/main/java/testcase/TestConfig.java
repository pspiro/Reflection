package testcase;


import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import reflection.Config;
import tw.util.S;

public class TestConfig extends MyTestCase {
	//static String host = "34.125.38.193"; //http://reflection.trading";
	static String host = "localhost"; //http://reflection.trading";
	
	public void testConnection() throws Exception {
		JsonObject map = cli().get("/api/status").readJsonObject();
		assert200();
		assertEquals( true, map.getBool("IB") );
		assertEquals( true, map.getBool("TWS") );
	}
	
	public void testRefreshConfig() throws Exception {
		cli().get("/api/?msg=refreshconfig");
		assert200();
	}

	public void testBackendConfigs() throws Exception {
		JsonArray ar = cli().get("/api/faqs").readJsonArray();
		S.out( ar.getJsonObj(0) );
		assertTrue( ar.size() > 3);

		JsonObject obj = cli().get("/api/system-configurations/last").readJsonObject();
		S.out( obj.get("min_order_size"));
		assertTrue( obj.size() > 5);
		
		obj = cli().get("/api/configurations").readJsonObject();
		S.out( obj);
		assertTrue( obj.size() > 5);
		
		obj = cli().get("/api/configurations?key=whitepaper_text").readJsonObject();
		S.out( obj);
		assertEquals(1, obj.size() );
	}
	
	public void testSendEmail() throws Exception {
		m_config.sendEmail("peteraspiro@gmail.com", "abc", "def");		
	}
	
	
	
}
