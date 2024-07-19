package testcase;

import common.Util;
import tw.util.S;

public class TestReward extends MyTestCase {
	public void test() throws Exception {
		Cookie.setNewFakeAddress(false);
		
		var pers = Util.toJson(
				"status", "completed",
				"fields", Util.toJson( "name", "bob")
				);
		
		var body = Util.toJson( 
				"wallet_public_key", Cookie.wallet,
				"cookie", Cookie.cookie,
				"persona_response", pers);
		
		// first time with new wallet should get $500
		S.out( "sending for wallet " + Cookie.wallet);
		var resp = cli().postToJson( "/api/users/register", body.toString() );
		resp.display();
		assert200();
		assertStartsWith( "$500", cli.getMessage() );

		// second time with same wallet should not
		S.out( "sending for wallet " + Cookie.wallet);
		resp = cli().postToJson( "/api/users/register", body.toString() );
		resp.display();
		assert200();
		assertEquals( "", cli.getMessage() );
		
	}

}
