package testcase;

import common.Util;
import http.MyClient;
import junit.framework.TestCase;
import reflection.Config;

public class TestPwServer extends TestCase {
	
	public void test() throws Exception  {
		var json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "pro", 
				"code", "prodco"
				).toString() );
		assertEquals( "Invalid name", json.getString( "error") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "prod", 
				"code", "prodco"
				).toString() );
		assertEquals( "Invalid code", json.getString( "error") );

//		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
//				"name", "prod", 
//				"code", "prodcode"
//				).toString() );
//		assertEquals( "Invalid source IP '127.0.0.1'", json.getString( "error") );

		json = MyClient.postToJson( "http://localhost:888/getpw", Util.toJson( 
				"name", "prod", 
				"code", "prodcode"
				).toString() );
		assertEquals( "mypw", json.getString( "pw") );
	}
	
	public void testPulse() throws Exception {
		Config c = Config.ask( "pulse");
		c.ownerKey();
	}
}
