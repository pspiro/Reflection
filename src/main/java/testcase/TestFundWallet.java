package testcase;

import org.json.simple.JsonObject;

import common.Util;

public class TestFundWallet extends MyTestCase {
	public void testFailWallet() throws Exception {
		Cookie.setNewFakeAddress(true);
		var json = getJson().append( "wallet_public_key", ""); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no wallet

		Cookie.setNewFakeAddress(true);
		json = getJson().append( "wallet_public_key", "0x9393"); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, invalid wallet
	}
	
	public void test500() throws Exception {
		Cookie.setNewFakeAddress(true);
		var json = getJson().append( "amount", 500); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no KYC
		
		Cookie.setNewFakeAddress(true, true);
		json = getJson().append( "amount", 500); 
		cli().postToJson( "/api/fund-wallet", json);
		assert200(); // pass 500 w/ KYC
	}
	
	public void test() throws Exception {
		Cookie.setNewFakeAddress(true);
		cli().postToJson( "/api/fund-wallet", getJson() );
		assert200(); // success

		cli().postToJson( "/api/fund-wallet", getJson() );
		assert400(); // second time w/ same wallet fails

		Cookie.setNewFakeAddress(true);
		var json = getJson().append( "cookie", ""); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, no cookie
		
		Cookie.setNewFakeAddress(false);
		cli().postToJson( "/api/fund-wallet", getJson() );
		assert400();  // fail, no user profile
		
		Cookie.setNewFakeAddress(true);
		json = getJson().append( "amount", 200); 
		cli().postToJson( "/api/fund-wallet", json);
		assert400(); // fail, invalid amount
	}
	
	public void testRusd() throws Exception {
		Cookie.setNewFakeAddress(true);
		m_config.rusd().mintRusd( Cookie.wallet, 2, stocks.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance(Cookie.wallet, 2, false);
		cli().postToJson( "/api/fund-wallet", getJson() );
		assert400();  // fail, RUSD in wallet
		
		m_config.rusd().burnRusd( Cookie.wallet, 1.5, stocks.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance(Cookie.wallet, 1.5, true);
		cli().postToJson( "/api/fund-wallet", getJson() );
		assert200();  // pass, 50 cents is okay
	}
	
	JsonObject getJson() {
		return Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"amount", 100);
	}
}
