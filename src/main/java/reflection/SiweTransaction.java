package reflection;

import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.util.Utils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import json.MyJsonObject;
import tw.util.S;

public class SiweTransaction extends MyTransaction {
	private static final HashSet<String> validNonces = new HashSet<>();
	static final HashMap<String,Session> sessionMap = new HashMap<>();  // map   
					// it would be better to store this in the Redis; give it a key so you can see or wipe out all 
	
	static class Session {
		private String m_nonce;
		private long m_lastTime;
		
		public Session(String nonce) {
			m_nonce = nonce;
			m_lastTime = System.currentTimeMillis();
		}

		public String nonce() {
			return m_nonce;
		}
		
		public long lastTime() {
			return m_lastTime;
		}

		public void update() {
			m_lastTime = System.currentTimeMillis();
		}
		
	}
	
	public SiweTransaction(Main main, HttpExchange exch) {
		super(main,exch);
	}

	/** Frontend requests nonce to build SIWE message.
	 *  Generate the nonce, save it, and return it */
	public void handleSiweInit() {
		String nonce = Utils.generateNonce();
		
		// save the nonce; only saved nonces are valid in the signin message, and only once 
		validNonces.add( nonce);
		
		respond( "nonce", nonce );   // how to tie this to the nonce received below?
	}
	
	/** Frontend sends POST msg with siwe message and signature; we should verify
	 *  The response contains a Set-Cookie header with the SIWE message and signature */
	public void handleSiweSignin() {
		wrap( () -> {
            MyJsonObject signedMsg = new MyJsonObject(
            		new JSONParser().parse( new InputStreamReader( m_exchange.getRequestBody() ) ) );  // or we could call parseMsg() here, but it's working this way
            
            S.out( "Received signed msg: %s", signedMsg);
			
			SiweMessage siweMsg = signedMsg.getRequiredObj( "message").getSiweMessage();
			
			// validate and remove the nonce so it is no longer valid
			Main.require( 
					validNonces.remove(siweMsg.getNonce() ), 
					RefCode.INVALID_REQUEST, 
					"The nonce %s is invalid", siweMsg.getNonce() );
			
			// verify signature
			if (signedMsg.getString( "signature").equals("102268") && siweMsg.getChainId() == 5) {
				// free pass, used for testing
			}
			else {
				// better would be to catch it and throw RefException; this return UNKNOWN code
				siweMsg.verify(siweMsg.getDomain(), siweMsg.getNonce(), signedMsg.getString( "signature") );  // we should not take the domain and nonce from here. pas
			}
			
			// verify issuedAt is not too far in future or past
			Instant issuedAt = Instant.from( DateTimeFormatter.ISO_INSTANT.parse( siweMsg.getIssuedAt() ) );
			Instant now = Instant.now();
			Main.require(
					Duration.between( issuedAt, now).toMillis() <= Main.m_config.siweTimeout(),
					RefCode.TIMED_OUT,
					"Oops, you waited a bit too long; please sign in again");
					// The 'issuedAt' time on the SIWE login request is too far in the past  issuedAt=%s  now=%s  max=%s",					+ "
					//issuedAt, now, Main.m_config.siweTimeout() );
			Main.require(
					Duration.between( now, issuedAt).toMillis() <= Main.m_config.siweTimeout(),
					RefCode.TIMED_OUT,
					"The 'issuedAt' time on the SIWE login request too far in the future  issuedAt=%s  now=%s  max=%s",
					issuedAt, now, Main.m_config.siweTimeout() );
			
			// store session object; let the nonce be the key for the session 
			Session session = new Session( siweMsg.getNonce() );
			sessionMap.put( siweMsg.getAddress(), session);
		
			// create the cookie to send back in the 'Set-Cookie' message header
			String cookie = String.format( "__Host_authToken%s%s=%s",
					siweMsg.getAddress(), 
					siweMsg.getChainId(), 
					URLEncoder.encode(signedMsg.toString() ) );
			
			HashMap<String,String> headers = new HashMap<>();
			headers.put( "Set-Cookie", cookie);
			
			respondFull( Util.toJsonMsg( code, "OK"), 200, headers);
		});
	}
	
	/** Frontend sends GET request with the cookie from the signin message sent in the header
	 *  This is a keep-alive; we should verify that the timer has not expired */
	public void handleSiweMe() {
		wrap( () -> {
			// check for cookie header
			String cookie = findCookie( m_exchange.getRequestHeaders(), "__Host_authToken");
			S.out( "  cookie:" + cookie);
			
			// no cookie 
			if (cookie == null) {
				failedMe( "No cookie header in request");
				return;
			}
			
			// the cookie has two parts: tag=value
			// the tag is __Host_authToken<address><chainid>
			// the value is json with two fields, signature and message

			// parse cookie
			Main.require( cookie.split("=").length >= 2, RefCode.INVALID_REQUEST, "Malformed cookie: " + cookie);

			MyJsonObject signedSiweMsg = MyJsonObject.parse( URLDecoder.decode(cookie.split("=")[1]) );
			MyJsonObject siweMsg = signedSiweMsg.getObj("message");
			Main.require( siweMsg != null, RefCode.INVALID_REQUEST, "Malformed cookie: " + cookie);
			
			// find session object
			Session session = sessionMap.get( siweMsg.getString("address") );
			if (session == null) {
				failedMe( "No session object found for address " + siweMsg.getString("address") );
				return;
			}
			
			// validate nonce
			if (!session.nonce().equals( siweMsg.getString("nonce") ) ) {
				failedMe( "Nonce " + siweMsg.getString("nonce") + " does not match " + session.nonce() );
				return;
			}
			
			// check expiration
			if (System.currentTimeMillis() - session.lastTime() > Main.m_config.sessionTimeout() ) {
				failedMe( "Session has expired");
				return;
			}
			
			// update expiration time
			session.update();
			
			S.out( "  loggedIn=true");
			JSONObject response = new JSONObject();
			response.put("loggedIn", true);
			response.put("message", siweMsg);
			respond(response);
		});

	}
	
	void handleSiweSignout() {
		respondOk();
	}
	
	void failedMe(String text) {
		S.out( "  " + text);
		respondFull( Util.toJsonMsg( "loggedIn", false, "message", text), 400, null);
	}

	public static void main(String[] args) {
		
	}
	
	/** Find the cookie header that starts with name */
	static String findCookie(Headers headers, String name) {
		if (headers != null) {
			List<String> listOfCookies = headers.get( "Cookie");
			if (listOfCookies != null) {
				for (String cookies : listOfCookies) {
					StringTokenizer st = new StringTokenizer(cookies, ";");
					while (st.hasMoreTokens() ) {
						String cookie = st.nextToken().trim();
						if (cookie.startsWith(name) ) {
							return cookie;
						}
					}
				}
			}
		}
		return null;
	}

}
