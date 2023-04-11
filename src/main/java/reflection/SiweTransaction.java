package reflection;

import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.util.Utils;
import com.sun.net.httpserver.HttpExchange;

import json.MyJsonObject;
import tw.util.S;

public class SiweTransaction extends MyTransaction {
	public SiweTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}

	/** Frontend requests nonce to build SIWE message */
	public void handleSiweInit() {
		S.out( "Handling /siwi/init");
		respond( "nonce", Utils.generateNonce() );   // how to tie this to the nonce received below?
	}
	
	/** Frontend sends POST msg with siwe message and signature; we should verify
	 *  The response contains a Set-Cookie header with the SIWE message and signature */
	public void handleSiweSignin() {
		wrap( () -> {
			S.out( "Handling /siwe/signin");
			
            MyJsonObject signedMsg = new MyJsonObject(
            		new JSONParser().parse( new InputStreamReader( m_exchange.getRequestBody() ) )
            );
            
            S.out( "Received signed msg: %s", signedMsg);
			
			MyJsonObject msgObj = signedMsg.getObj( "message");
			SiweMessage siweMsg = msgObj.getSiweMessage();
			S.out( "**error, verify is disabled**");
			//siweMsg.verify(siweMsg.getDomain(), siweMsg.getNonce(), signedMsg.getString( "signature") );  // we should not take the domain and nonce from here. pas
			
			String cookieVal = String.format( "__Host_authToken%s%s=%s",
					siweMsg.getAddress(), 
					siweMsg.getChainId(), 
					URLEncoder.encode(signedMsg.toString() ) 
			);
			
			HashMap<String,String> headers = new HashMap<>();
			headers.put( "Set-Cookie", cookieVal);
			
			respondFull( Util.toJsonMsg( code, "OK"), 200, headers);
		});
	}
	
	/** Frontend sends GET request with the cookie returned in the signin message.
	 *  This is a keep-alive; we should verify that the timer has not expired */
	public void handleSiweMe() {
		wrap( () -> {
			S.out( "Handling /siwe/me");
			
			S.out( "headers");
			S.out(m_exchange.getRequestHeaders());			

			List<String> headers = m_exchange.getRequestHeaders().get("Cookie");
			Main.require(headers.size() == 1, RefCode.INVALID_REQUEST, "Wrong number of Cookies in header: " + headers.size() );
			
			String cookie = headers.get(0);
			Main.require( cookie.split("=").length == 2, RefCode.INVALID_REQUEST, "Invalid SIWE cookie " + cookie);
			String signedMsgJson = URLDecoder.decode( cookie.split("=")[1]);
			MyJsonObject obj = MyJsonObject.parse(signedMsgJson);			
			S.out( "Received signed message in cookie:");
			S.out( signedMsgJson);
			
			JSONObject response = new JSONObject();
			response.put("loggedIn", true);
			response.put("message", obj);
			respond( new Json(response) );
		});

	}

}
