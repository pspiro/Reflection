package reflection;

import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.util.Utils;
import com.sun.net.httpserver.HttpExchange;

import json.MyJsonObject;
import tw.util.S;

public class SiweTransaction extends MyTransaction {
	static HashSet<String> m_validNonces = new HashSet<>();
	
	public SiweTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}

	/** Frontend requests nonce to build SIWE message.
	 *  Generate the nonce, save it, and return it */
	public void handleSiweInit() {
		S.out( "Handling /siwi/init");
		String nonce = Utils.generateNonce();
		
		// save the nonce; only saved nonces are valid in the signin message, and only once 
		m_validNonces.add( nonce);
		
		respond( "nonce", nonce );   // how to tie this to the nonce received below?
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
			
			SiweMessage siweMsg = signedMsg.getRequiredObj( "message").getSiweMessage();
			
			// validate and remove the nonce so it is no longer valid
			Main.require( 
					m_validNonces.remove(siweMsg.getNonce() ), 
					RefCode.INVALID_REQUEST, 
					"The nonce %s is invalid", siweMsg.getNonce()
			);
			
			// verify signature
			if (!signedMsg.getBool("test") ) {
				siweMsg.verify(siweMsg.getDomain(), siweMsg.getNonce(), signedMsg.getString( "signature") );  // we should not take the domain and nonce from here. pas
			}
			
			// verify time is not too far in future or past
			Instant createdAt = Instant.from( DateTimeFormatter.ISO_INSTANT.parse( siweMsg.getIssuedAt() ) );
			Main.require(
					Duration.between( createdAt, Instant.now() ).toMillis() <= Main.m_config.siweTimeout(),
					RefCode.TIMED_OUT,
					"The 'issuedAt' time on the SIWE login request too far in the past"
			);

			Main.require(
					Duration.between( Instant.now(), createdAt).toMillis() <= Main.m_config.siweTimeout(),
					RefCode.TIMED_OUT,
					"The 'issuedAt' time on the SIWE login request too far in the future"
			);
		
			// create the cookie to send back in the 'Set-Cookie' message header
			String cookie = String.format( "__Host_authToken%s%s=%s",
					siweMsg.getAddress(), 
					siweMsg.getChainId(), 
					URLEncoder.encode(signedMsg.toString() ) 
			);
			
			HashMap<String,String> headers = new HashMap<>();
			headers.put( "Set-Cookie", cookie);
			
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
			response.put("message", obj.getObj("message").toJsonObj() );
			respond( new Json(response) );
		});

	}

}
