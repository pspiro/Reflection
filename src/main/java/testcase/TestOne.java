package testcase;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;
import reflection.SiweUtil;
import reflection.Util;
import tw.util.S;

public class TestOne extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String cookie;
	static double curPrice = 135.75; // Double.valueOf( jedis.hget("8314", "last") );
	
	static {
		try {
			signIn(wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testFracShares: %s: %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	static String orderData(double offset) {
		return orderData( offset, "buy", "0x0xb016711702D3302ceF6cEb62419abBeF5c44450e");
	}
	
	static String orderData(double offset, String side, String cryptoId) {
		return String.format( "{ 'conid': '8314', 'action': '%s', 'quantity': '100', 'price': '%s', 'cryptoid': '%s', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'tds': 1.11 }",
				side, Double.valueOf( curPrice + offset), cryptoId );
	}
	
	static MyJsonObject sendData( String data) throws Exception {
		signIn(wallet);
		
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.addHeader("Cookie", cookie).post( "/api/reflection-api/order", Util.toJson(data) );
		return cli.readMyJsonObject();
	}

	private static void signIn(String address) throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		
		// send siwe/init
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");

		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				address, 
				"http://localhost", 
				"1",	// version 
				5,      // chainId 
				nonce,
				Util.isoNow() )
				.statement("Sign in to Reflection.")
				.build();
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", "102268");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		
		cookie = cli.getHeaders().get("set-cookie");
	}
}
