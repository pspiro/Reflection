package testcase;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.error.SiweException;

import chain.Chains;
import common.Util;
import reflection.RefCode;
import reflection.SiweUtil;
import tw.util.S;

public class TestSiwe extends MyTestCase {
	/* 
	 * Client			Server
	 * 		siweInit -->
	 * 		<-- nonce
	 * 
	 * 		siweSignin -->
	 * 		  (signed Siwe msg including nonce and wallet address)
	 * 					validate nonce
	 * 					validate signature
	 * 
	 * 		<-- set-cookie header
	 * 		  (signed Siwe msg)
	 * 
	 * 		siweMe w/ cookie header -->
	 * 		  (containing signed Siwe msg)
	 * 	
	 * 	* The nonce becomes the session key
	 *  * Backend must have a concept of whether the session is valid or not, and reject msgs if not
	 *  * Backend must update the last used time; how to persist that between sessions?
	 *  * Let nonce expiration be configurable
	 *  
	 *  Alternative to the whole thing:
	 *  * no login, no session
	 *  * we validate every time before each order; should be super-quick
	 *  * now, the validation becomes the 
	 *  * there might be better ways to sign the message than SIWE, other libs that give you more control
	 *  
	 *   DO NOT USE Cookie in this class since it signs in and messes things up
	 */

	static String myWalletAddress = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String signature = "102268";  // special sig that will always pass

	
	static SiweMessage createSiweMsg(String nonce, Instant time) throws SiweException {  
		return new SiweMessage.Builder(
			"Reflection.trading", 
			myWalletAddress, 
			"http://localhost", 
			"1", 
			Chains.Sepolia,      // chainId 
			nonce,
			DateTimeFormatter.ISO_INSTANT.format(time) )
			.statement("Sign in to Reflection.")
			.build();
	}

	public void testFailSessionExpired() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce

		// the server uses the time on the message itself, not the actual elapsed time
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now().minusSeconds(70) );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "past " + cli.readJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.TOO_SLOW, cli.getRefCode() );
	}
	
	public void testSiweFailFut() throws Exception {
		// test siwe/init
		cli = cli();
		cli.get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now().plusSeconds(70) );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "fut " + cli.readJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.TOO_FAST, cli.getRefCode() );
	}
	
	public void testFailSig() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now().plusSeconds(22) );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature + "a");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli().post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failSig " + cli.readJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.UNKNOWN, cli.getRefCode() );  // gives unknown because it is a Siwe exception; better would be to catch it and throw RefException
	}
	
	public void testFailDup() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failDup " + cli.readJsonObject() );
		assert200();

		// test siwe/signin again
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failDup " + cli.readJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
	}

	// won't work on Mock server since it doesn't read config
	public void testFailTimeout() throws Exception {
		modifySetting( "sessionTimeout", "2000", () -> {
			
			// send siwe/init
			cli().get("/siwe/init");
			assert200();
			String nonce = cli.readJsonObject().getString("nonce");
			assertEquals( 20, nonce.length() );
	
			// create siwe message
			SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
			JsonObject signedMsgSent = new JsonObject();
			signedMsgSent.put( "signature", signature);
			signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );
	
			// send siwe/signin
			cli().post("/siwe/signin", signedMsgSent.toString() );
			assert200();
			String cookie = cli.getHeaders().get("set-cookie");
			
			S.sleep(1000);

			// send siwe/me
			cli().post("/siwe/me", Util.toJson( "nonce", nonce, "address", myWalletAddress) );
			assert200();
	
			S.sleep(2500);
			
			// fail siwe/me
			cli().post("/siwe/me", Util.toJson( "nonce", nonce, "address", myWalletAddress) );
			S.out( cli.readJsonObject() );
			assertEquals( 200, cli.getResponseCode() );
			assertFalse( cli.readJsonObject().getBool("loggedIn") );
		});
	}
	
	public void testSiweSignout() throws Exception {
		// siwe/init, get nonce
		String nonce = cli().get("/siwe/init").readJsonObject().getString("nonce");
		assert200();
		
		SiweMessage siweMsg = TestSiwe.createSiweMsg(nonce, Instant.now() );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", TestSiwe.signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// siwe/signin
		cli().post("/siwe/signin", signedMsgSent.toString() ).getHeaders().get("set-cookie");
		
		// test successful siwe/me
		cli().post("/siwe/me", Cookie.getJson() ).assertResponseCode(200);
		
		// sign out
		cli().post("/siwe/signout", Cookie.getJson() ).assertResponseCode(200);
		
		// test failed siwe/me
		var re = cli().postToJson("/siwe/me", Cookie.getJson() );
		assertFalse( re.getBool( "loggedIn") );
	}
	
	public void testSiweSignin() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli().post("/siwe/signin", signedMsgSent.toString() );
		assert200();
		
		// test successful siwe/me
		var resp = cli().postToJson("/siwe/me", Util.toJson( 
				"nonce", nonce,
				"address", myWalletAddress) );
		S.out( "me " + cli.readString() );
		assert200();
		assertTrue( resp.getBool("loggedIn") );
	}
	
	/** This is to test that the cookie and nonce can survive a RefAPI restart */
	public void testRestartRefAPI() throws Exception {
		Cookie.setNewFakeAddress(false);
		
		cli().post("/siwe/me", Cookie.getJson() );
		assert200();

		S.out( "Restart RefAPI");
		Util.pause();

		// still works
		cli().post("/siwe/me", Cookie.getJson() );
		assert200();
		
		// fails with wrong cookie
		cli().post("/siwe/me", Util.toJson( "nonce", "abc"));
		assert400();
	}
	
	public void testFailCookie() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );

		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli().post("/siwe/signin", signedMsgSent.toString() );

		// test siwe/me w/ no cookie
		cli().post("/siwe/me", Util.toJson() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		
		// test a successful me
		S.sleep(500);
		cli().post("/siwe/me", Cookie.getJson() );
		assert200();
	}
	
	// v2
	public void testSignIn2() throws Exception {
		// test siwe/init
		S.out( "sending init");
		cli().getJson("/siwe/init").display();
		assert200();
		String nonce = cli.readJsonObject().getString("nonce");
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);  // get a real signature, you can do that, now
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		S.out( "signint in");
		cli().postToJson("/siwe/signin", signedMsgSent.toString() ).display();
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		// test successful siwe/me
		S.out( "testing with /me");
		var me = cli().postToJson("/siwe/me", Util.toJson( "address", myWalletAddress, "nonce", nonce) );
		assert200();
		assertEquals( true, me.getBool("loggedIn") );
		
		// logout
		S.out( "sign out");
		cli().postToJson("/siwe/signout", Util.toJson( "address", myWalletAddress) );
		assert200();
		
		S.out( "testing /me failure");
		me = cli().postToJson("/siwe/me", Util.toJson( "address", myWalletAddress, "nonce", nonce) );
		assert200();
		assertEquals( false, me.getBool("loggedIn") );
	}	
}