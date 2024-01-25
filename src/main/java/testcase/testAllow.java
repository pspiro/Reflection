package testcase;

import org.json.simple.JsonObject;

import http.MyClient;

/** allow connection based on country code and ip address */
public class testAllow extends MyTestCase {
	static String ip = "838.838.838.838";
	
	public void testMyIp() throws Exception {
		String str = MyClient.create("http://localhost:8383/api/allowConnection")
			.header( "X-Country-Code", "US")
			.header("X-Real-IP", ip)
			.query().body();
		
		JsonObject obj = JsonObject.parse( str);
			
		assertTrue( obj.getBool("allow") );
		assertEquals( ip, obj.getString("ip") );
		assertEquals( "US", obj.getString("country") );
	}
}
