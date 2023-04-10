package reflection;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import tw.util.S;

public class TestSiwe {
	public static void main(String[] args) throws Exception {
		String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";

		S.out( "-----Sending siwe/init");
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		cli.get("/siwe/init");

		String nonce = cli.readMyJsonObject().getString("nonce");
		S.out( "Received nonce: %s", nonce);

		// (will be wrong)
		String signature = "0xb704d00b0bd15e789e26e566d668ee03cca287218bd6110e01334f40a38d9a8377eece1d958fff7a72a5b669185729a18c1a253fd0ddcf9711764a761d60ba821b";

		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				wallet, 
				"https://usedapp-docs.netlify.app", 
				"1", 
				5, 
				nonce, 
				"2023-04-10T14:40:03.878Z")
				.statement("Sign in to Reflection.")
				.build();
		
		JSONObject msg = new JSONObject();
		msg.put( "domain", siweMsg.getDomain() );
		msg.put( "address", siweMsg.getAddress() );
		msg.put( "URI", siweMsg.getUri() );
		msg.put( "version", siweMsg.getVersion() );
		msg.put( "chainId", siweMsg.getChainId() );
		msg.put( "nonce", siweMsg.getNonce() );
		msg.put( "issuedAt", siweMsg.getIssuedAt() );
		msg.put( "statement", siweMsg.getStatement() );
		
		JSONObject signedMsg = new JSONObject();
		signedMsg.put( "signature", signature);
		signedMsg.put( "message", msg);

		S.out( "-----Sending siwe/signin:");
		S.out( signedMsg);
		MyHttpClient cli2 = new MyHttpClient("localhost", 8383);
		cli2.post("/siwe/signin", signedMsg.toString() );

		String resp2 = cli2.readString();
		S.out( "Received response: %s", resp2);

		String cookie2 = cli2.getHeaders().get("set-cookie");
		S.out( "Reveived cookie: %s", cookie2);
		
		String signedMsg2 = cookie2.split("=")[1];
		S.out( "Received signed msg: %s", signedMsg2);
		// verify that the cookie is correct
		
		//----------------------------
		S.out( "-----Sending siwe/me");
		
		MyHttpClient cli3 = new MyHttpClient("localhost", 8383);
		cli3.addHeader("cookie", cookie2);
		cli3.get("/siwe/me");
		S.out( "Receved:");
		MyJsonObject resp3 = cli3.readMyJsonObject();
		resp3.display();
		
	}
}
