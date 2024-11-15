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
	
	/** test onramp API directly; you need Main running to set the cookie */
	public void testGetKycUrl() throws Exception {
		Cookie.setNewFakeAddress(true);
		String phone = newPhone();
		
		// first time
		S.out( "json1***");
		JsonObject json = Onramp.devRamp.getKycUrlFirst( Cookie.wallet, phone, "http://redirect");
		json.display();
		assertTrue( json.has( "url", custId));

		// second time, pass ID only
		S.out( "json4***");
		var json4 = Onramp.devRamp.getKycUrlNext( json.getString( custId), "http://redirect");
		assertTrue( json4.has( "url", custId));
		assertEquals( json.getString(custId), json4.getString(custId) );
	}

	public void testRefApi() throws Exception {
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
		S.out( "convert-1");
		var resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt"),
				"test", true
				));
		assert200();
		assertNotNull( resp.getString( "url") ); 
		assertNotNull( resp.getString( "customerId") );
		
		S.out( "convert-2");
		resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200();
		assertNotNull( resp.getString( "url") ); 
		assertNotNull( resp.getString( "customerId") );
		
		S.out( "enter OTP to progress:");
		S.out( "url is:\n" + resp.getString( "url") );
		Util.pause();

		Onramp.devRamp.updateKycStatus(resp.getString( "customerId"), KycStatus.BASIC_KYC_COMPLETED);

		S.out( "convert-3");
		resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt")
				));
		assert200();  // failing 400 - UNKNOWN - Cannot invoke "chain.Chain.params()" because the return value of "chain.Chains.polygon()" is null
		startsWith( "The transaction has been", cli.getMessage() );
		assertTrue( resp.has( "createdAt") );
		assertTrue( resp.has( "bank") );
		assertTrue( resp.has( "amount") );
	}
	
	public void testExistingWallet() throws Exception {
		S.out( "testExistingWallet");
		Cookie.setWalletAddr("0x7c3e1c7291DDF2045e5Fc0C61a3e9cc5E28Dd783"); // set a wallet that has onramp id and passed kyc (set it manually from 

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
		var resp = cli().postToJson( "/api/onramp-convert", Util.toJson(
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"currency", "EUR", 
				"buyAmt", 3000,
				"recAmt", quote.getDouble( "recAmt"),
				"test", true
				));
		resp.display();
		startsWith( "The transaction has been", cli.getMessage() );
		assertNotNull( resp.get( "amount") );
		assertNotNull( resp.get( "message") );
		assertNotNull( resp.get( "createdAt") );
		assertNotNull( resp.get( "bank") );
	}
	
	public static String newPhone() {
		return "+44-" + Util.rnd.nextLong( 2172845679L, 7172845679L);
	}
// try one more time in test system	
}
