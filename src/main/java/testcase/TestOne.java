package testcase;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.RefCode;
import reflection.SiweUtil;
import tw.util.S;

public class TestOne extends MyTestCase {
	public void testSiweSignin() throws Exception {
		// test siwe/init
		MyHttpClient cli = cli();
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );  // confirm nonce
		
		SiweMessage siweMsg = TestSiwe.createSiweMsg(nonce, Instant.now() );
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", TestSiwe.signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// test siwe/signin
		cli = cli();
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		String cookie = cli.getHeaders().get("set-cookie");
		assertTrue( cookie != null && cookie.split("=").length >= 2);
		
		// confirm values returned
		MyJsonObject signedMsgRec = MyJsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
		assertEquals( TestSiwe.signature, signedMsgRec.getString("signature") );
		MyJsonObject msg3 = signedMsgRec.getObj("message");
		assertEquals( TestSiwe.myWalletAddress, msg3.getString("address"));
		assertEquals( nonce, msg3.getString("nonce"));

		// test successful siwe/me
		cli = cli();
		cli.addHeader("Cookie", "mycookie=abcde; " + cookie)  // how is this working???
			.get("/siwe/me");
		S.out( "me " + cli.readString() );
		assertEquals( 200, cli.getResponseCode() );
		MyJsonObject meResponseMsg = cli.readMyJsonObject();
		assertTrue( meResponseMsg.getBool("loggedIn") );
		MyJsonObject meSiweMsg = meResponseMsg.getObj("message");
		assertEquals( TestSiwe.myWalletAddress, meSiweMsg.getString("address") );
		assertEquals( nonce, meSiweMsg.getString("nonce"));
	}
}
