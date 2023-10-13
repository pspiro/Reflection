package testcase;

import org.json.simple.JsonObject;

import http.MyClient;
import tw.util.S;

public class TestMktDataServer extends MyTestCase {
	String base = "https://reflection.trading/mdserver";
	
	public void testStatus() throws Exception {
		JsonObject json = MyClient.getJson( base + "/status");
		assertEquals( "OK", json.getString("OK") ); 
		assertEquals( "OK", json.getString("TWS") ); 
		assertEquals( "OK", json.getString("IB") );
		assertTrue( json.getInt("mdCount") > 0);
	}

	public void testDesubscribe() throws Exception {
		assertEquals( 200, MyClient.getResponse( base + "/desubscribe").statusCode() );
	}

	public void testDisconnect() throws Exception {
		assertEquals( 200, MyClient.getResponse( base + "/disconnect").statusCode() );
		assertEquals( "OK", MyClient.getJson( base + "/status").getString("TWS") );
		S.sleep(10000);
		assertEquals( "OK", MyClient.getJson( base + "/status").getString("TWS") );
	}

}
