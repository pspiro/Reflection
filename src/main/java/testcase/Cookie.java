package testcase;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import junit.framework.Assert;
import junit.framework.TestCase;
import reflection.SiweUtil;
import reflection.Util;

public class Cookie extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String cookie;

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
	}


}
