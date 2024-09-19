package testcase;

import org.json.simple.JsonObject;

import common.Util;
import onramp.Onramp;
import tw.util.S;

public class TestOnramp extends MyTestCase {
	static String custId = "customerId";
	
//	public void testOk() throws Exception {
//		assertEquals( 4, Onramp.waitForOrderStatus(819754, 10) );
//
//		cli().postToJson("http://localhost:8383/api/onramp", Util.toJson( 
//				"wallet_public_key", Cookie.wallet,
//				"orderId", 819754).toString() ).display();
//		assert200();
//	}
//	
//	public void testFail() throws Exception {
//		assertEquals( -101, Onramp.waitForOrderStatus(81975, 10) );
//
//		cli().postToJson("http://localhost:8383/api/onramp", Util.toJson( 
//				"wallet_public_key", Cookie.wallet,
//				"orderId", 81975).toString() ).display();
//		assertEquals( RefCode.ONRAMP_FAILED, cli.getRefCode() ); 
//		assertEquals( 400, cli.getResponseCode() );
//	}
//	
//	public void testOnramp() throws Exception {
//		cli().postToJson( "http://localhost:8383/api/onramp", Util.toJson( 
//				"wallet_public_key", Cookie.wallet,
//				"orderId", 333).toString() ).display();
//		assert200_();
//	}
	
	/** test onramp API directly */
	public void testGetKycUrl() throws Exception {
		Cookie.setNewFakeAddress(true);
		String phone = newPhone();
		
		// first time
		S.out( "json1***");
		JsonObject json = Onramp.getKycUrlFirst( Cookie.wallet, phone, "http://redirect");
		assertTrue( json.has( "url", custId, "status"));

		// change wallet - fails with misleading message about the phone number
		S.out( "json2***");
		try {
			Onramp.getKycUrlFirst( Cookie.wallet, phone, "http://redirect");
			assertTrue( false);
		}
		catch( Exception e) {
		}

		// change phone - okay to reuse the same wallet!
		S.out( "json3***");
		try {
			Onramp.getKycUrlFirst( Cookie.wallet, newPhone(), "http://redirect");
		}
		catch( Exception e) {
		}

		// second time, pass ID only
		S.out( "json4***");
		var json4 = Onramp.getKycUrlNext( json.getString( custId), "http://redirect");
		assertEquals( json.getString(custId), json4.getString(custId) );
	}

	public void testApi() throws Exception {
		Cookie.setNewFakeAddress(true);
		
		// get quote
		var quote = cli().post( "/api/onramp-get-quote", Util.toJson( 
				"currency", "INR", 
				"buyAmt", 10000
				)).readJsonObject();
		S.out( "got quote " + quote);
		assertTrue( quote.getDouble( "recAmt") > 0);

		// convert, first time, creates new onramp id
		S.out( "waiting");
		cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "INR", 
				"buyAmt", 10000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200_();
		S.out( "rec " + cli.getMessage() );
		
		S.out( "waiting2");
		cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "INR", 
				"buyAmt", 10000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200_();
		S.out( "rec " + cli.getMessage() );
	}
	
	private String newPhone() {
		return "+44-" + Util.rnd.nextLong( 2172845679L, 7172845679L);
	}
// try one more time in test system	
}
