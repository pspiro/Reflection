package testcase;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class TestFundWallet extends MyTestCase {
	static String email = Util.uid( 5).toLowerCase() + "@gmail.com";
	
	static {
	}
	
	public void testFailWallet() throws Exception {
		Cookie.setNewFakeAddress(true);
		var json = getJson(email).append( "wallet_public_key", ""); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no wallet

		Cookie.setNewFakeAddress(true);
		json = getJson(email).append( "wallet_public_key", "0x9393"); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, invalid wallet
	}
	
	public void test500() throws Exception {
		Cookie.setNewFakeAddress(true);
		var json = getJson(email).append( "amount", 500);
		
		cli().postToJson( "/api/signup", getSignJson(email) );
		S.sleep( 200);
		
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no KYC
		
		Cookie.setNewFakeAddress(true, true);
		json = getJson(email).append( "amount", 500); 
		cli().postToJson( "/api/fund-wallet", json);
		assert200(); // pass 500 w/ KYC
	}
	
	public void test200() throws Exception {
		Cookie.setNewFakeAddress(true);
		
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert400(); // fail, no signup entry
		
		cli().postToJson( "/api/signup", getSignJson(email) );
		assert200(); // signup
		
		// fail, got prize
		S.out( "setting got_prize=true for email %s", email);
		m_config.sqlCommand( sql -> sql.execWithParams("update signup set got_prize = true where email = '%s'", email) );
		S.out( "funding wallet %s", email);
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert400(); 

		// success
		m_config.sqlCommand( sql -> sql.execWithParams("update signup set got_prize = false where email = '%s'", email) );
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert200();

		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert400(); // second time w/ same wallet fails

		Cookie.setNewFakeAddress(true);
		var json = getJson(email).append( "nonce", ""); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no cookie
		
		Cookie.setNewFakeAddress(false);
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert400();  // fail, no user profile
		
		Cookie.setNewFakeAddress(true);
		json = getJson(email).append( "amount", 200); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, invalid amount
	}
	
	public void testRusd() throws Exception {
		Cookie.setNewFakeAddress(true);
		m_config.rusd().mintRusd( Cookie.wallet, 2, chain.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance(Cookie.wallet, 2, false);
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert400();  // fail, RUSD in wallet
		
		m_config.rusd().burnRusd( Cookie.wallet, 1.5, chain.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance(Cookie.wallet, 1.5, true);
		cli().postToJson( "/api/fund-wallet", getJson(email) );
		assert200();  // pass, 50 cents is okay
	}
	
	JsonObject getJson(String email) {
		return Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"firstName", "first",
				"lastName", "last",
				"email", email,
				"chainId", Cookie.chainId(),
				"nonce", Cookie.nonce,
				"amount", 100);
	}
	
	JsonObject getSignJson(String email) {
		return Util.toJson( 
				"first", "first",
				"last", "last",
				"email", email);
	}
}
