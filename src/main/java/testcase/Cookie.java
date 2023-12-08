package testcase;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyHttpClient;
import reflection.SiweUtil;
import tw.util.S;

public abstract class Cookie extends MyTestCase {
	//public static String wallet = "0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3";
	//public static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	//public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	//public static String wallet = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
	//public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	public static String wallet = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
	public static String cookie;  // that's right, the cookie is a string, not an object
	public static boolean init; // to force initialization
	
	static {
		try {
			cookie = signIn(wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setNewWallet(String walletIn) throws Exception {
		wallet = walletIn;
		cookie = signIn(walletIn);
	}

	public static String signIn(String address) throws Exception {
		S.out( "Signing in with cookie for wallet " + address);
		
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		
		// send siwe/init
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readJsonObject().getString("nonce");

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
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", "102268");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		
		return cli.getHeaders().get("set-cookie");
		//S.out( "received cookie: " + cookie);
	}
}
