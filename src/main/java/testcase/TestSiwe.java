package testcase;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.error.SiweException;

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
			5,      // chainId 
			nonce,
			DateTimeFormatter.ISO_INSTANT.format(time) )
			.statement("Sign in to Reflection.")
			.build();
	}

	public void testFailSessionExpired() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200_();
		String nonce = cli.readJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce

		// the server uses the time on the message itself, not the actual elapsed time
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now().minusSeconds(22) );
		
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
		assert200_();
		String nonce = cli.readJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now().plusSeconds(22) );
		
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
		assert200_();
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
		assert200_();
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
		assert200_();

		// test siwe/signin again
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failDup " + cli.readJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
	}
	
	public void testFailTimeout() throws Exception {
		modifySetting( "sessionTimeout", "2000", () -> {
			
			// send siwe/init
			cli().get("/siwe/init");
			assert200_();
			String nonce = cli.readJsonObject().getString("nonce");
			assertEquals( 20, nonce.length() );
	
			// create siwe message
			SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
			JsonObject signedMsgSent = new JsonObject();
			signedMsgSent.put( "signature", signature);
			signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );
	
			// send siwe/signin
			cli().post("/siwe/signin", signedMsgSent.toString() );
			assert200_();
			String cookie = cli.getHeaders().get("set-cookie");
			
			S.sleep(1000);

			// send siwe/me
			cli().addHeader("Cookie", cookie).get("/siwe/me");
			assert200_();
	
			S.sleep(2500);
			
			// fail siwe/me
			cli().addHeader("Cookie", cookie).get("/siwe/me");
			S.out( cli.readJsonObject() );
			assertEquals( 400, cli.getResponseCode() );
			assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
			assertEquals( false, cli.readJsonObject().getBool("loggedIn") );
		});
	}
	
	public void testSiweSignout() throws Exception {
		// siwe/init, get nonce
		String nonce = cli().get("/siwe/init").readJsonObject().getString("nonce");
		assert200_();
		
		SiweMessage siweMsg = TestSiwe.createSiweMsg(nonce, Instant.now() );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", TestSiwe.signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// siwe/signin, get cookie
		String cookie = cli().post("/siwe/signin", signedMsgSent.toString() ).getHeaders().get("set-cookie");
		
		// test successful siwe/me
		cli().addHeader("Cookie", cookie).get("/siwe/me").assertResponseCode(200);
		
		// sign out
		cli().addHeader("Cookie", cookie).get("/siwe/signout").assertResponseCode(200);
		
		// test failed siwe/me
		cli().addHeader("Cookie", cookie).get("/siwe/me").assertResponseCode(400);
	}
	
	public void testSiweSignin() throws Exception {
		// test siwe/init
		cli = cli();
		cli.get("/siwe/init");
		assert200_();
		String nonce = cli.readJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce
		
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assert200_();
		String cookie = cli.getHeaders().get("set-cookie");
		assertTrue( cookie != null && cookie.split("=").length >= 2);
		
		// confirm values returned
		JsonObject signedMsgRec = JsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
		assertEquals( signature, signedMsgRec.getString("signature") );
		JsonObject msg3 = signedMsgRec.getObject("message");
		assertEquals( myWalletAddress, msg3.getString("address"));
		assertEquals( nonce, msg3.getString("nonce"));

		// test successful siwe/me
		cli = cli();
		cli.addHeader("Cookie", "mycookie=abcde; " + cookie)  // add an unrelated cookie for fun
			.get("/siwe/me");
		S.out( "me " + cli.readString() );
		assert200_();
		JsonObject meResponseMsg = cli.readJsonObject();
		assertTrue( meResponseMsg.getBool("loggedIn") );
		JsonObject meSiweMsg = meResponseMsg.getObject("message");
		assertEquals( myWalletAddress, meSiweMsg.getString("address") );
		assertEquals( nonce, meSiweMsg.getString("nonce"));
				
		// test another successful me but include an invalid cookie, which should be okay
		String badCookie = String.format( "__Host_authToken%s5=abc", TestSiwe.myWalletAddress);
		cookie = badCookie + "; " + cookie;
				
		cli = cli();
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		S.out( "me " + cli.readString() );
		assert200_();
		meResponseMsg = cli.readJsonObject();
		assertTrue( meResponseMsg.getBool("loggedIn") );
		meSiweMsg = meResponseMsg.getObject("message");
		assertEquals( TestSiwe.myWalletAddress, meSiweMsg.getString("address") );
		assertEquals( nonce, meSiweMsg.getString("nonce"));
	}
	
	/** This is to test that the cookie and nonce can survive a RefAPI restart */
	public void testRestartRefAPI() throws Exception {
		Cookie.setNewFakeAddress(false);
		
		cli().post( "/api/get-profile/" + Cookie.wallet, Util.toJson("cookie", Cookie.cookie).toString() ); 
		assert200_();

		S.out( "Restart RefAPI");
		Util.pause();

		// missing cookie
		cli().get( "/api/get-profile/" + Cookie.wallet);
		assertEquals( 400, cli.getResponseCode() );

		cli().post( "/api/get-profile/" + Cookie.wallet, Util.toJson("cookie", Cookie.cookie).toString() ); 
		assert200_();
	}
	
	public void testFailCookie() throws Exception {
		// test siwe/init
		cli().get("/siwe/init");
		assert200_();
		String nonce = cli.readJsonObject().getString("nonce");
		SiweMessage siweMsg = createSiweMsg(nonce, Instant.now() );

		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		String cookie = cli.getHeaders().get("set-cookie");
		//JsonObject signedMsgRec = JsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
		//JsonObject msg3 = signedMsgRec.getObject("message");

		// test siwe/me w/ no cookie
		cli = cli();
		cli.get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		startsWith( "Null cookie", cli.getMessage() );
		
		// test mismatched cookie header and body address
		JsonObject badSignedObj = JsonObject.parse("""
				 { "signature": "signature", "message": { "address": "0x0000000000000000000000000000000000000001" } }
				""");
		
		String badCookie = String.format( "__Host_authToken0x00000000000000000000000000000000000000005=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = cli();
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		//startsWith( "Cookie header wallet address", cli.getMessage() );

		// test cookie not found
		badCookie = String.format( "__Host_authToken0x00000000000000000000000000000000000000015=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = cli();
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		//startsWith( "No session object found", cli.getMessage() );
		
		// test siwe/me wrong nonce
		badSignedObj = JsonObject.parse("""
				 { "signature": "signature", "message": { "address": "0xb016711702D3302ceF6cEb62419abBeF5c44450e", "nonce": "badnonce" } }
				""");		
		badCookie = String.format( "__Host_authToken0xb016711702D3302ceF6cEb62419abBeF5c44450e5=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = cli();
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
		//startsWith( "Cookie nonce", cli.getMessage() );

		// test a successful me
		S.sleep(500);
		cli = cli();
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		assert200_();
	}
	
}