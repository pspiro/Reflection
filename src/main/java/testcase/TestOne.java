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

public class TestOne extends TestCase {
	public void testFailTimeout() throws Exception {
		// send siwe/init
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");
		assertEquals( 20, nonce.length() );

		// create siwe message
		SiweMessage siweMsg = TestSiwe.msg(nonce, Instant.now() );
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", TestSiwe.signature);
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		String cookie = cli.getHeaders().get("set-cookie");
		
		// send successful siwe/me
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		assertEquals( 200, cli.getResponseCode() );

		S.sleep(2100);
		
		// fail siwe/me
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("Cookie", cookie).get("/siwe/me");
		S.out( cli.readMyJsonObject() );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( false, cli.readMyJsonObject().getBool("loggedIn") ); 
	}
}
