package testcase;

import common.Util;
import tw.util.S;

public class TestFaucet extends MyTestCase {
	public void testShow() throws Exception {
		// no user profile
		Cookie.setNewFakeAddress(false);
		
		// show-faucet still succeeds
		S.out( "show faucet success");
		var json = cli().postToJson("/api/show-faucet/" + Cookie.wallet, Cookie.getJson() );
		assert200(); 
		assertEquals( json.getDouble( "amount"), m_config.chain().params().faucetAmt() );

		// fails, no user id
		S.out( "fail show faucet");
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert400();

		// with user profile
		Cookie.setNewFakeAddress(true);

		// show faucet
		S.out( "show faucet succeed - " + Cookie.wallet);
		double amount = cli().postToJson("/api/show-faucet/" + Cookie.wallet, Cookie.getJson() )
				.getDouble( "amount");
		assert200(); 
		assertEquals( m_config.chain().params().faucetAmt(), amount);
		
		// fund wallet
		S.out( "turn faucet succeed");
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert200(); // succeed
		waitFor(30, () -> m_config.node().getNativeBalance(Cookie.wallet) == amount); // confirm balance in wallet
		S.out( "  received native token");
		
		// fail second time
		S.out( "fail turn faucet");
		cli().postToJson("/api/turn-faucet", Util.toJson(
				"cookie", Cookie.cookie,
				"wallet_public_key", Cookie.wallet))
			.getDouble( "amount");
		assert400();
		
		// should no longer show faucet (returns zero)
		double amt2 = cli().postToJson("/api/show-faucet/" + Cookie.wallet, Cookie.getJson() ).getDouble( "amount");
		assert200(); 
		assertEquals( 0.0, amt2);
		
		// confirm database entry
		assertEquals( amount, Cookie.getUser()
				.getObjectNN( "locked")
				.getObjectNN( "faucet")
				.getDouble( "Sepolia") );
		
	}

}
