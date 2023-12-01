package testcase;

import org.json.simple.JsonObject;

import http.MyClient;
import tw.util.S;

public class TestMktDataServer extends MyTestCase {
	String base = "http://localhost:6989/mdserver";
	
	public void testStatus() throws Exception {
		JsonObject json = MyClient.getJson( base + "/status");
		S.out(json);
		assertEquals( true, json.getBool("TWS") ); 
		assertEquals( true, json.getBool("IB") ); 
		assertEquals( "OK", json.getString("code") ); 
		assertTrue( json.getInt("mdCount") > 0);
		assertTrue( json.getLong("started") > 0);
	}

	public void testSubscribe() throws Exception {
		assertEquals( 200, MyClient.getResponse( base + "/subscribe").statusCode() );
	}

	public void testDesubscribe() throws Exception {
		assertEquals( 200, MyClient.getResponse( base + "/desubscribe").statusCode() );
	}

	/** Cause a disconnec, and see that MdServer can reconnect okay */
	public void testReconnect() throws Exception {
		assertEquals( "true", MyClient.getJson( base + "/status").getString("TWS") );
		assertEquals( 200, MyClient.getResponse( base + "/disconnect").statusCode() );
		S.sleep(10);
		assertEquals( "false", MyClient.getJson( base + "/status").getString("TWS") );
		S.sleep(5000);
		assertEquals( "true", MyClient.getJson( base + "/status").getString("TWS") );
	}

}
