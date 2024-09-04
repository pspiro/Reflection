package testcase;

import org.json.simple.JsonObject;

import common.Util;

public class TestCheckIdentity extends MyTestCase {
	public void testFailNoCookie() throws Exception {
		JsonObject json = Util.toJson( 
				"wallet_public_key", Cookie.wallet.toLowerCase() );
		
		cli().postToJson("/api/check-identity", json.toString() );
		assertEquals( 400, cli.getResponseCode() );
	}

	public void testVerified() throws Exception {
		JsonObject json = Util.toJson( 
				"wallet_public_key", Cookie.wallet.toLowerCase(),
				"cookie", Cookie.cookie);
		
		m_config.sqlCommand( sql -> sql.execWithParams( "update users set kyc_status = 'VERIFIED' where wallet_public_key = '%s'",
				Cookie.wallet.toLowerCase() ) );
		
		JsonObject ret = cli().postToJson("/api/check-identity", json.toString() );
		ret.display();
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( true, ret.getBool( "verified") );
	}

	public void testNotVerified() throws Exception {
		JsonObject json = Util.toJson( 
				"wallet_public_key", Cookie.wallet.toLowerCase(),
				"cookie", Cookie.cookie);
		
		m_config.sqlCommand( sql -> sql.execWithParams( "update users set kyc_status = '' where wallet_public_key = '%s'",
				Cookie.wallet.toLowerCase() ) );
		
		JsonObject ret = cli().postToJson("/api/check-identity", json.toString() );
		ret.display();
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( false, ret.getBool( "verified") );
	}

}
