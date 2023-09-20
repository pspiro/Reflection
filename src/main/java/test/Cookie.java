package test;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyAsyncClient;
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
	public static Object lock = new Object();
	
	public static void init() {
		signIn(wallet);
	}

	public static void signIn(String address) {
		S.out( "Signing in with cookie for wallet " + address);
		MyAsyncClient.getJson( base + "/siwe/init", json -> gotNonce( json.getString("nonce"), address) );
	}
	
	static void gotNonce(String nonce, String address) throws Exception {
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
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
		.prepare("POST", base + "/siwe/signin")
		.setBody(signedMsgSent.toString())
		.execute()
		.toCompletableFuture()
		.thenAccept( obj -> {
			try {
				client.close();
				cookie = obj.getHeader("set-cookie");
				S.out( "set cookie");
				synchronized( lock) {
					lock.notify();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
