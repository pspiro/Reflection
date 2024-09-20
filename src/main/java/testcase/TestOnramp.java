package testcase;

import org.json.simple.JsonObject;

import common.Util;
import onramp.Onramp;
import onramp.Onramp.KycStatus;
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
		assertTrue( json.has( "kycUrl", custId));

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
		S.out( "getquote");
		var quote = cli().postToJson( "/api/onramp-get-quote", Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"currency", "EUR", 
				"buyAmt", 3000
				));
		S.out( "got quote " + quote);
		assertTrue( quote.getDouble( "recAmt") > 0);

		// convert, first time, creates new onramp id
		S.out( "testapi1");
		var resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200_();
		assertNotNull( resp.getString( "url") ); 
		assertNotNull( resp.getString( "customerId") );
		
		S.out( "testapi2");
		resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200_();
		assertNotNull( resp.getString( "url") ); 
		assertNotNull( resp.getString( "customerId") );
		
		S.out( "enter OTP to progress:");
		S.out( "url is:\n" + resp.getString( "url") );
		Util.pause();

		Onramp.updateKycStatus(resp.getString( "customerId"), KycStatus.BASIC_KYC_COMPLETED);

		S.out( "testapi3");
		resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200_();
		startsWith( "The transaction has been initiated", cli.getMessage() );
	}
	
	private String newPhone() {
		return "+44-" + Util.rnd.nextLong( 2172845679L, 7172845679L);
	}
// try one more time in test system	
}
