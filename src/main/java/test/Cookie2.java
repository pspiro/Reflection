package test;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyAsyncClient;
import reflection.SiweUtil;
import tw.util.S;

public class Cookie2 {
	String base;
	private String m_addr;
	private String m_cookie;
	
	String cookie() { return m_cookie; }
	String address() { return m_addr; }
	
	Cookie2( String baseIn) {
		base = baseIn;
	}
	
	public void signIn(String address, Runnable run) {
		m_addr = address;
		
		S.out( "Signing in with cookie2 for wallet " + address);
		MyAsyncClient.getJson( base + "/siwe/init", json -> {
			SiweMessage siweMsg = new SiweMessage.Builder(
					"Reflection.trading", 
					address, 
					base, 
					"1",	// version 
					5,      // chainId 
					json.getString("nonce"),
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
						m_cookie = obj.getHeader("set-cookie");
						run.run();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				});
		});
	
	}
}
