package testcase;

import org.json.simple.JsonObject;

import common.Util;
import onramp.Onramp;
import reflection.RefCode;
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
	
	public void testSimplePass() throws Exception {
		String wallet = Util.createFakeAddress();
		String phone = "+91-" + Util.rnd.nextInt( 1000, 1000000);
		
		JsonObject json;
		
		S.out( "---------------");
		json = Onramp.getKycUrl( wallet, phone);
		json.display();
		assertTrue( json.has( "url"));
		assertTrue( json.has( custId));
		assertTrue( json.has( "status"));
		
		S.out( "---------------");
		var json2 = Onramp.getKycUrl( json.getString( custId), wallet, phone);
		json2.display();
		assertEquals( json.getString(custId), json2.getString(custId) ); 
		assertEquals( json.getString("status"), json2.getString("status") );
		assertTrue( json.has( "status"));
	}
	
	public void testChangePhone() throws Exception {
		String wallet = Util.createFakeAddress();
		String phone = "+91-" + Util.rnd.nextInt( 1000, 1000000);
		
		JsonObject json;
		
		S.out( "-------- part 1 --------");
		json = Onramp.getKycUrl( wallet, phone);
		json.display();
		assertTrue( json.has( "url"));
		assertTrue( json.has( custId));
		assertTrue( json.has( "status"));
		
		S.out( "-------part 2--------");
		// changing the phone is okay; it just creates a new cust id
		var json2 = Onramp.getKycUrl( json.getString( custId), wallet, phone + 1);
		json2.display();
		assertNotEquals( json.getString( custId), json2.getString( custId) ); 
		
		// no phone
		try {
			json2 = Onramp.getKycUrl( json.getString( custId), wallet, "");
			json2.display();
			assertTrue( false);
		}
		catch( Exception e) {}
	}
	
	public void testChangeWallet() throws Exception {
		String wallet = Util.createFakeAddress();
		String phone = "+91-" + Util.rnd.nextInt( 1000, 1000000);
		
		JsonObject json;
		
		S.out( "-------- part 1 --------");
		json = Onramp.getKycUrl( wallet, phone);
		json.display();
		assertTrue( json.has( "url"));
		assertTrue( json.has( custId));
		assertTrue( json.has( "status"));
		
		// this is a bug in the onramp api; you can pass any value except 
		// for null for the wallet the second time
		try {
			var json2 = Onramp.getKycUrl( json.getString(custId), "abc", phone);
			json2.display();
			assertEquals( json.getString(custId), json2.getString(custId) ); 
			assertEquals( json.getString("status"), json2.getString("status") );
			//assertTrue( false); // should fail but doesn't
		}
		catch( Exception e) {}

		// no wallet
		try {
			var json2 = Onramp.getKycUrl( json.getString( custId), phone);
			json2.display();
			assertTrue( false);
		}
		catch( Exception e) {}
	}
	
	public void testChangeCustId() throws Exception {
		String wallet = Util.createFakeAddress();
		String phone = "+91-" + Util.rnd.nextInt( 1000, 1000000);
		
		JsonObject json;
		
		S.out( "-------- part 1 --------");
		json = Onramp.getKycUrl( wallet, phone);
		json.display();
		assertTrue( json.has( "url"));
		assertTrue( json.has( custId));
		assertTrue( json.has( "status"));

		// change cust id
		S.out( "---------------");
		try {
			var json2 = Onramp.getKycUrl( "Z" + json.getString( custId), wallet, phone);
			json2.display();
			assertTrue( false);
		}
		catch( Exception e) {}
		
		// remove cust id
		S.out( "---------------");
		try {
			var json2 = Onramp.getKycUrl( "", wallet, phone);
			json2.display();
			assertTrue( false);
		}
		catch( Exception e) {}
	}
}
