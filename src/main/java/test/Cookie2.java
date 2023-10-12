package test;

import java.net.http.HttpResponse;

import org.json.simple.JsonObject;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyClient;
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
	
	public void signIn(String address) throws Exception {
		m_addr = address;
		
		S.out( "Signing in with cookie2 for wallet " + address);
		
		// send init, get nonce
		JsonObject json = MyClient.getJson( base + "/siwe/init");

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
		HttpResponse<String> resp = MyClient
				.create( base + "/siwe/signin", signedMsgSent.toString() )
				.query();
		m_cookie = resp.headers().firstValue("set-cookie").get();
	}
}
