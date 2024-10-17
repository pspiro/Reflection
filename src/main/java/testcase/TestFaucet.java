package testcase;

import common.Util;

public class TestFaucet extends MyTestCase {
	public void testShow() throws Exception {
		// no user profile
		Cookie.setNewFakeAddress(false);
		
		// show-faucet still succeeds
		var json = cli().getJson("/api/show-faucet/" + Cookie.wallet);
		assert200(); 
		assertEquals( json.getDouble( "amount"), m_config.faucetAmt() );

		// fails, no user id
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert400();

		// with user profile
		Cookie.setNewFakeAddress(true);

		// show faucet
		double amount = cli().getJson("/api/show-faucet/" + Cookie.wallet)
				.getDouble( "amount");
		assert200(); 
		assertEquals( m_config.faucetAmt(), amount);
		
		// fund wallet
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert200(); // succeed
		waitFor(30, () -> m_config.node().getNativeBalance(Cookie.wallet) == amount); // confirm balance in wallet
		
		// fail second time
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert400();
		
		// should no longer show faucet (returns zero)
		double amt2 = cli().getJson("/api/show-faucet/" + Cookie.wallet).getDouble( "amount");
		assert200(); 
		assertEquals( 0.0, amt2);
		
		// confirm database entry
		assertEquals( amount, Cookie.getUser()
				.getObjectNN( "locked")
				.getObjectNN( "faucet")
				.getDouble( m_config.blockchainName() ) );
		
	}

}
