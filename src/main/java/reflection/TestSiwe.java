package reflection;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import tw.util.S;

public class TestSiwe {
	public static void main(String[] args) throws Exception {
		String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";

		S.out( "-----Sending GET siwe/init");
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");
		String nonce = cli.readMyJsonObject().getString("nonce");
		Util.require( cli.getResponseCode() == 200, "Received error response code: " + cli.getResponseCode() );
		S.out( "Received nonce: %s", nonce);

		// (will be wrong)
		String signature = "0xb704d00b0bd15e789e26e566d668ee03cca287218bd6110e01334f40a38d9a8377eece1d958fff7a72a5b669185729a18c1a253fd0ddcf9711764a761d60ba821b";

		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				wallet, 
				"https://reflection.trading", 
				"1", 
				5,      // chainId 
				nonce,
				DateTimeFormatter.ISO_INSTANT.format( Instant.now() ) )
				.statement("Sign in to Reflection.")
				.build();
		
		JSONObject signedMsg = new JSONObject();
		signedMsg.put( "signature", signature);
		signedMsg.put( "message", SiweUtil.toJsonObject(siweMsg) );

		S.out( "-----Sending POST siwe/signin: %s", signedMsg);
		MyHttpClient cli2 = new MyHttpClient("localhost", 8383);
		cli2.post("/siwe/signin", signedMsg.toString() );
		
		S.out( "Received response: %s", cli2.readString() );
		S.out( "Received response code: %s", cli2.getResponseCode() );
		Util.require( cli2.getResponseCode() == 200, "Received error response code: " + cli2.getResponseCode() );

		String cookie2 = cli2.getHeaders().get("set-cookie");
		Util.require( S.isNotNull( cookie2), "No cookie returned from signin");
		
		S.out( "Reveived cookie: %s", cookie2);
		
		String signedMsg2 = cookie2.split("=")[1];
		S.out( "Received signed msg: %s", signedMsg2);
		// verify that the cookie is correct
		
		//----------------------------
		S.out( "-----Sending siwe/me");
		
		MyHttpClient cli3 = new MyHttpClient("localhost", 8383);
		cli3.addHeader("cookie", cookie2);
		cli3.get("/siwe/me");
		
		MyJsonObject resp3 = cli3.readMyJsonObject();
		Util.require( cli3.getResponseCode() == 200, "Received error response code: " + cli3.getResponseCode() );
		resp3.display("Received:");
		
	}
}
