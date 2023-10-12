package test;

import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyClient;
import reflection.SiweUtil;
import tw.util.S;

public class Cookie {
	//public static String wallet = "0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3";
	//public static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	//public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	//public static String wallet = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
	public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	public static String cookie;  // that's right, the cookie is a string, not an object
	public static String base = "https://reflection.trading";

	public static void init() {
		Util.wrap( () -> signIn(wallet) );
	}

	static void signIn(String address) throws Exception {
		S.out( "Signing in with cookie1 for wallet " + address);
		String nonce = MyClient.getJson( base + "/siwe/init").getString("nonce");

		// send siwe/init, get nonce
		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				address, 
				base, 
				"1",	// version 
				5,      // chainId 
				nonce,
				Util.isoNow() )
				.statement("Sign in to Reflection.")
				.build();

		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", "102268");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		HttpResponse<String> resp = MyClient
				.create( base + "/siwe/signin", signedMsgSent.toString() )  
				.query();
		cookie = resp.headers().firstValue("set-cookie").get();
		S.out( "set cookie");
	}
}
