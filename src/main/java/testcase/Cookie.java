package testcase;

import java.net.URLDecoder;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.Assert;
import junit.framework.TestCase;
import reflection.SiweUtil;
import reflection.Util;
import tw.util.S;

public class Cookie extends TestCase {
	//public static String wallet = "0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3";
	public static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	public static String cookie;

	static {
		try {
			signIn(wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void signIn(String address) throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		
		// send siwe/init
		cli.get("/siwe/init");
		Assert.assertEquals( 200, cli.getResponseCode() );
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
		S.out( "received cookie: " + cookie);
	}
	
	public static void main(String[] args) throws Exception {
		String str = "{\"smartcontractid\":\"0x5195729466e481de3c63860034fc89efa5fbbb8f\",\"symbol\":\"AAPL (Apple)\",\"tds\":0,\"quantity\":0.030117,\"cookie\":\"__Host_authToken0xb95bf9C71e030FA3D8c0940456972885DB60843F5=%7B%22address%22%3A%220xb95bf9C71e030FA3D8c0940456972885DB60843F%22%2C%22chainId%22%3A5%2C%22domain%22%3A%22localhost%22%2C%22statement%22%3A%22Sign%20in%20with%20Ethereum.%22%2C%22issuedAt%22%3A%222023-04-18T16%3A38%3A50.330Z%22%2C%22uri%22%3A%22http%3A%2F%2Flocalhost%22%2C%22version%22%3A%221%22%2C%22nonce%22%3A%22lQ7ew6PhuZzMSy1eQNRX%22%7D\",\"tokenPrice\":166.0202,\"spread\":0.006,\"price\":6,\"action\":\"buy\",\"currency\":\"busd\",\"commission\":1,\"conid\":265598,\"wallet_public_key\":\"0xb95bf9C71e030FA3D8c0940456972885DB60843F\",\"timestamp\":0}";
		MyJsonObject obj = MyJsonObject.parse(str);
		String cookie = obj.getString("cookie");
		//S.out(cookie);
		MyJsonObject siweMsg = MyJsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) )
				.getObj("message");
		
	}
	
	static MyJsonObject addCookie(MyJsonObject obj) throws Exception {
		obj.put("cookie", Cookie.cookie);
		obj.put("noFireblocks", true);
		obj.put("currency", "busd");
		obj.put("wallet_public_key", Cookie.wallet);
		
		double price = obj.getDouble("tokenPrice");
		double qty = obj.getDouble("quantity");
		double comm = obj.getDouble("commission");
		double total = obj.getString("action").equals("buy")
				? price * qty + comm : price * qty - comm;
		obj.put("price", total);
		return obj;
	}

}
