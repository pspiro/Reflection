package testcase;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.error.SiweException;

import http.MyHttpClient;
import json.MyJsonObject;
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
	 */

	static String myWalletAddress = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String signature = "102268";  // special sig that will always pass

	
	static SiweMessage msg(String nonce, Instant time) throws SiweException {  
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

	public void testSiweFailPast() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = msg(nonce, Instant.now().minusSeconds(22) );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "past " + cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( "TIMED_OUT", cli.getCode() );
	}
	
	public void testSiweFailFut() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = msg(nonce, Instant.now().minusSeconds(22) );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "fut " + cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( "TIMED_OUT", cli.getCode() );
	}
	
	public void testFailSig() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = msg(nonce, Instant.now().plusSeconds(22) );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature + "a");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failSig " + cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( "UNKNOWN", cli.getCode() );  // gives unknown because it is a Siwe exception; better would be to catch it and throw RefException
	}
	
	public void testFailDup() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		
		// confirm nonce
		assertEquals( 20, nonce.length() );
		
		SiweMessage siweMsg = msg(nonce, Instant.now() );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failDup " + cli.readMyJsonObject() );
		assertEquals( 200, cli.getResponseCode() );

		// test siwe/signin again
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		S.out( "failDup " + cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
	}
	
	public void testFailTimeout() throws Exception {
		// send siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );

		// create siwe message
		SiweMessage siweMsg = msg(nonce, Instant.now() );
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		String cookie = cli.getHeaders().get("set-cookie");
		
		// send siwe/me
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		assertEquals( 200, cli.getResponseCode() );

		S.sleep(3000);
		
		// fail siwe/me
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		S.out( cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( false, cli.readMyJsonObject().getBool("loggedIn") ); 
	}
	
	public void testSiweSignin() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce
		
		SiweMessage siweMsg = msg(nonce, Instant.now() );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		String cookie = cli.getHeaders().get("set-cookie");
		assertTrue( cookie != null && cookie.split("=").length >= 2);
		
		// confirm values returned
		MyJsonObject signedMsgRec = MyJsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
		assertEquals( signature, signedMsgRec.getString("signature") );
		MyJsonObject msg3 = signedMsgRec.getObj("message");
		assertEquals( myWalletAddress, msg3.getString("address"));
		assertEquals( nonce, msg3.getString("nonce"));

		// test successful siwe/me
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", "mycookie=abcde; " + cookie).get("/siwe/me");
		S.out( "me " + cli.readString() );
		assertEquals( 200, cli.getResponseCode() );
		MyJsonObject meResponseMsg = cli.readMyJsonObject();
		assertTrue( meResponseMsg.getBool("loggedIn") );
		MyJsonObject meSiweMsg = meResponseMsg.getObj("message");
		assertEquals( myWalletAddress, meSiweMsg.getString("address") );
		assertEquals( nonce, meSiweMsg.getString("nonce"));
				
		// test another successful me
		S.sleep(500);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		assertEquals( 200, cli.getResponseCode() );
	}
	
	public void testFailCookie() throws Exception {
		// test siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		String nonce = cli.readMyJsonObject().getString("nonce");
		SiweMessage siweMsg = msg(nonce, Instant.now() );

		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		String cookie = cli.getHeaders().get("set-cookie");
		MyJsonObject signedMsgRec = MyJsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
		MyJsonObject msg3 = signedMsgRec.getObj("message");

		// test siwe/me w/ no cookie
		cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( "No cookie header in /siwe/me request", cli.getMessage() );
		
		// test mismatched cookie header and body address
		JSONObject badSignedObj = MyJsonObject.parse("""
				 { "signature": "signature", "message": { "address": "0x0000000000000000000000000000000000000001" } }
				""").toJsonObj();
		
		String badCookie = String.format( "__Host_authToken0x00000000000000000000000000000000000000005=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Header address", cli.getMessage() );

		// test cookie not found
		badCookie = String.format( "__Host_authToken0x00000000000000000000000000000000000000015=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "No session object found", cli.getMessage() );
		
		// test siwe/me wrong nonce
		badSignedObj = MyJsonObject.parse("""
				 { "signature": "signature", "message": { "address": "0xb016711702D3302ceF6cEb62419abBeF5c44450e", "nonce": "badnonce" } }
				""").toJsonObj();		
		badCookie = String.format( "__Host_authToken0xb016711702D3302ceF6cEb62419abBeF5c44450e5=%s", URLEncoder.encode(badSignedObj.toString() ) );
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("cookie", badCookie).get("/siwe/me");
		assertEquals( 400, cli.getResponseCode() );
		startsWith( "Cookie nonce", cli.getMessage() );

		// test a successful me
		S.sleep(500);
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		assertEquals( 200, cli.getResponseCode() );
	}
	
}
