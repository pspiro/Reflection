package testcase;

import common.Util;
import http.MyClient;
import reflection.SingleChainConfig;

public class TestPwServer extends MyTestCase {
	String code = "lwjkefdj827";
	
	public void testLocal() throws Exception  {
		var json = MyClient.getJson( "http://localhost:888/ok"); 
		assertEquals( "OK", json.getString( "code") );

		json = MyClient.postToJson( "http://localhost:888/", Util.toJson( 
				"name", "pro", 
				"code", code
				).toString() );
		assertEquals( "No such endpoint", json.getString( "error") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "pro", 
				"code", code
				).toString() );
		assertEquals( "Invalid name", json.getString( "error") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "prod", 
				"code", "invalidCode"
				).toString() );
		assertEquals( "Invalid code", json.getString( "error") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "prod", 
				"code", code
				).toString() );
		assertEquals( "prodPw", json.getString( "pw") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "pulse", 
				"code", code
				).toString() );
		assertEquals( "pulsePw", json.getString( "pw") );
	}
	
	public void testProd() throws Exception {
		SingleChainConfig c = SingleChainConfig.ask( "pulse");
		c.admin1Key();
	}
	
	/** you have to remove the local ip for this test to pass 
	 * @throws Exception */
	public void testInvalidIp() throws Exception {
		var json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "prod", 
				"code", code
				).toString() );
		assertEquals( "Invalid source IP '127.0.0.1'", json.getString( "error") );
	}
}
