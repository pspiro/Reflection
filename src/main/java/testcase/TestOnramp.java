package testcase;

import common.Util;
import onramp.Onramp;
import reflection.RefCode;

public class TestOnramp extends MyTestCase {
	public void testOk() throws Exception {
		assertEquals( 4, Onramp.waitForOrderStatus(819754, 10) );

		cli().postToJson("http://localhost:8383/api/onramp", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", 819754).toString() ).display();
		assert200();
	}
	
	public void testFail() throws Exception {
		assertEquals( -101, Onramp.waitForOrderStatus(81975, 10) );

		cli().postToJson("http://localhost:8383/api/onramp", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", 81975).toString() ).display();
		assertEquals( RefCode.ONRAMP_FAILED, cli.getRefCode() ); 
		assertEquals( 400, cli.getResponseCode() );
	}
	
	public void testOnramp() throws Exception {
		cli().postToJson( "http://localhost:8383/api/onramp", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"orderId", 333).toString() ).display();
		assert200_();
	}
}
