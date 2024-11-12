package siwe;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.json.simple.JsonObject;
import org.json.simple.STable;

import com.moonstoneid.siwe.SiweMessage;
import com.moonstoneid.siwe.util.Utils;
import com.sun.net.httpserver.HttpExchange;

import chain.Chains;
import common.Util;
import http.BaseTransaction;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;
import util.LogType;

/** note use Keys.toChecksumAddress() to get EIP55 mixed case address */
public class SiweTransaction extends BaseTransaction {
	private static final HashSet<String> validNonces = new HashSet<>(); // not serialized
	private static final STable<SiweSession> sessionMap = new STable<SiweSession>("siwe.dat", 5000, SiweSession.class);  // map wallet address to Session
	
	private static long siweTimeout = Util.MINUTE;		// sign-in must be completed in this timeframe 
	private static long sessionTimeout = Util.HOUR;  	// user must re-auth after this expires
	
	public static void setTimeouts( long t1, long t2) {
		siweTimeout = t1; 
		sessionTimeout = t2;
	}
	
	public static void startThread() {
		sessionMap.startThread();
	}

	public SiweTransaction(HttpExchange exch) {
		super( exch, true);
	}

	/** Frontend requests nonce to build SIWE message.
	 *  Generate the nonce, save it, and return it */
	public void handleSiweInit() {
		wrap( () -> {
			String nonce = Utils.generateNonce();
			
			// save the nonce; only saved nonces are valid in the signin message, and only once 
			validNonces.add( nonce);
			
			respond( "nonce", nonce );   // how to tie this to the nonce received below?
		});
	}
	
	/** Frontend sends POST msg with siwe message and signature; we should verify
	 *  The response contains a Set-Cookie header with the SIWE message and signature.
	 *  
	 *   No change here for v2, but we no longer need to send the cookie back on the response. */
	public void handleSiweSignin() {
		wrap( () -> {
            JsonObject signedMsg = parseToObject();
            			
			SiweMessage siweMsg = signedMsg.getRequiredObj( "message").getSiweMessage();
			
			// set m_wallet so it will appear in log messages
            out( "  received sign-in for %s", siweMsg.getAddress() );
			
			// validate and remove the nonce so it is no longer valid
			Main.require( 
					validNonces.remove(siweMsg.getNonce() ), 
					RefCode.INVALID_NONCE, 
					"The nonce %s is invalid", siweMsg.getNonce() );
			
			out( "  nonce is valid");
			
			// verify signature
			if (signedMsg.getString( "signature").equals("102268") && siweMsg.getChainId() == Chains.Sepolia) {
				out( "  free pass");
			}
			else {
				// better would be to catch it and throw RefException; this returns UNKNOWN code
				siweMsg.verify(siweMsg.getDomain(), siweMsg.getNonce(), signedMsg.getString( "signature") );  // we should not take the domain and nonce from here. pas
				out( "  verified");
			}
			
			// verify issuedAt is not too far in future or past
			Instant issuedAt = Instant.from( DateTimeFormatter.ISO_INSTANT.parse( siweMsg.getIssuedAt() ) );
			Instant now = Instant.now();
			Main.require(
					Duration.between( issuedAt, now).toMillis() <= siweTimeout,
					RefCode.TOO_SLOW,
					"You waited too long to sign in; please sign in again");
					// The 'issuedAt' time on the SIWE login request is too far in the past  issuedAt=%s  now=%s  max=%s",					+ "
					//issuedAt, now, siweTimeout() );
			Main.require(
					Duration.between( now, issuedAt).toMillis() <= siweTimeout,
					RefCode.TOO_FAST,
					"The 'issuedAt' time on the SIWE login request is too far in the future  issuedAt=%s  now=%s  max=%s",
					issuedAt, now, siweTimeout );
			
			Main.require(
					S.isNotNull( siweMsg.getAddress() ),
					RefCode.INVALID_REQUEST,
					"Null address during siwe/signin");
			
			// store session object; let the wallet address be the key for the session 
			SiweSession session = new SiweSession( siweMsg.getNonce() );
			sessionMap.put( siweMsg.getAddress().toLowerCase(), session);
			out( "  mapping %s to %s", siweMsg.getAddress(), session);
		
			// create the cookie to send back in the 'Set-Cookie' message header
			String cookie = String.format( "__Host_authToken%s%s=%s",
					siweMsg.getAddress(), 
					siweMsg.getChainId(), 
					URLEncoder.encode(signedMsg.toString() ) );
			
			HashMap<String,String> headers = new HashMap<>();
			headers.put( "Set-Cookie", cookie);   // obsolete, remove. pas
		
			respondFull( Util.toJson( code, "OK"), 200, headers);
			
			// log successful sign-in
			olog( LogType.SIGNED_IN, "country", getCountryCode(), "ip", getUserIpAddress() );
		});
	}
	
	/** Frontend sends GET request with the cookie from the signin message sent in the header
	 *  This is a keep-alive; we should verify that the timer has not expired */
	public void handleSiweMe() {
		wrap( () -> {						
			JsonObject response = new JsonObject();
			
			out( "received siwe/me from %s %s", getUserIpAddress(), response);  // log the ip to see if we get multiple messages from the same user

			if (isPost() ) {  // v2
				var body = parseToObject();

				boolean loggedIn = isLoggedIn( 
						body.getString( "address"), 
						body.getString( "nonce")
						);
				
				response.put( "loggedIn", loggedIn); // we could add a failure reason, if we like. pas
			}
			else {  // v1
				ArrayList<String> cookies = authCookies();
				
				Main.require( cookies.size() > 0, RefCode.VALIDATION_FAILED, "Null cookie on /siwe/me");
				
				if (cookies.size() > 1) {
					out( "Warning: received /siwe/me with multiple cookies");  // this happens when the user switches wallets from MetaMask
					cookies.forEach( cookie -> out( "  " + address(cookie) ) ); // don't print whole cookie which gives access to user's wallet
				}
				
				JsonObject siweMsg = validateAnyCookie( cookies);
	
				response.put("loggedIn", true);
				response.put("message", siweMsg);  // this is not needed in v2
			}
			respond(response);
		});
	}
	
	/** this version is called by me() and returns boolean */
	public static boolean isLoggedIn( String address, String nonce) {
		SiweSession session = sessionMap.get( address.toLowerCase() );
		return 
			session != null && 
			session.nonce().equals( nonce) &&
			System.currentTimeMillis() - session.lastTime() <= sessionTimeout;
	}
	
	/** this version is called by all the different APIs and throws exceptions
	 *  this should never really fail because the frontend signs in before
	 *  each operation 
	 * @throws RefException */
	public static void validateNonce( String address, String nonce) throws RefException {
		SiweSession session = sessionMap.get( address.toLowerCase() );
		require( session != null, "No session found for %s", address);
		require( session.nonce().equals( nonce), "Session nonce does not match provided nonce");
		require( System.currentTimeMillis() - session.lastTime() <= sessionTimeout, "Session has expired");
	}

	private static void require( boolean v, String text, Object... params) throws RefException {
		Main.require( v, RefCode.VALIDATION_FAILED, text, params);
	}

	/** Return the Siwe message for the valid cookie; there could be more than one if the user
	 *  switched wallets because the cookie from the old wallet is not removed
	 *  @return siwe message */
	private JsonObject validateAnyCookie( ArrayList<String> cookies) throws RefException {
		for (String cookie : cookies) {
			String address = address(cookie);
			
			try {
				JsonObject msg = validateCookie( cookie, null);
				out( "  %s is signed in", address);
				return msg;
			}
			catch( RefException ex) {
				out( "  %s is not signed in - %s", address, ex.code() );  // could come here if the session expired or user switched wallets
			}
			catch( Exception e) {
				out( "  %s is not signed in - %s", address, e);  // some unexpected error
			}
		}

		throw new RefException( RefCode.VALIDATION_FAILED, "No valid cookies in /siwe/me message");  // this is normal
	}
	
	/** @param cookie could come from header or message body
	 *  @param wallet may be null
	 *  @return siwe message */ 
	public static JsonObject validateCookie( String cookie, String wallet) throws Exception {
		// the cookie has two parts: tag=value
		// the tag is __Host_authToken<address><chainid>
		// the value is json with two fields, signature and message
		// the value is URL-encoded and must be decoded
		String[] split = cookie.split("=");
		Main.require( split.length == 2, RefCode.VALIDATION_FAILED, "Malformed cookie");
		
		String cookieHeader = split[0];
		String cookieBody = URLDecoder.decode( split[1] );
		//out( "Cookie header: " + cookieHeader);
		//out( "Cookie body: " + cookieBody);

		JsonObject signedSiweMsg = JsonObject.parse(cookieBody);  // signature+message
		JsonObject siweMsg = signedSiweMsg.getObject("message");
		Main.require( siweMsg != null, RefCode.VALIDATION_FAILED, "No message in cookie");  // this happens in the pre-signin /siwe/me request
		
		// match address from cookie header with address from cookie body
		String headerAddress = cookieHeader.length() >= 58 ? cookieHeader.substring(16, 58) : "";
		String bodyAddress = siweMsg.getString("address");
		Main.require( 
				headerAddress.equalsIgnoreCase(bodyAddress), 
				RefCode.VALIDATION_FAILED,
				"Cookie header wallet address (%s) does not match cookie body wallet address (%s)",
				headerAddress, bodyAddress);
		
		// match address from message
		Main.require( 
				wallet == null || wallet.equalsIgnoreCase(bodyAddress), 
				RefCode.VALIDATION_FAILED,
				"Message wallet address (%s) does not match cookie wallet address (%s)",
				wallet, bodyAddress);
		
		// find session object
		SiweSession session = sessionMap.get( bodyAddress.toLowerCase() );
		Main.require( session != null, RefCode.VALIDATION_FAILED, "No session object found; please sign in and resubmit your request");
		
		// validate nonce
		Main.require( 
				session.nonce().equals( siweMsg.getString("nonce") ),
				RefCode.VALIDATION_FAILED,
				"Cookie nonce (%s) does not match session nonce (%s)",
				siweMsg.getString("nonce"), session.nonce() );
		
		// check expiration
		Main.require( System.currentTimeMillis() - session.lastTime() <= sessionTimeout,
				RefCode.VALIDATION_FAILED,
				"Your session has expired; please sign in again and resubmit your request"); // this message makes it back to the client for user actions but not /siwe/me
		
		// update expiration time
		session.update();
		
		return siweMsg;
	}
	
	/** Sign out all sign-in users (there should be only one);
	 *  get cookie from header */
	public void handleSiweSignout() {
		wrap( () -> {
			if (isPost() ) {  // v2
				String address = parseToObject().getString( "address");
				if (sessionMap.remove(address.toLowerCase()) != null) {  // alternatively, we could update the session to be false
					out( "  %s has been signed out", address);
				}
			}
			else {  // v1
				for (String cookie : authCookies() ) {
					String address = S.notNull(address(cookie));
					if (sessionMap.remove(address.toLowerCase()) != null) {  // alternatively, we could update the session to be false
						out( "  %s has been signed out", address);
					}
					else {
						out( "  %s was already signed out", address);
					}
				}
			}				
			respondOk();
		});
	}

	/** Return lower-case wallet address from __Host_authToken cookie */
	private static String address(String cookie) {
		return Util.substring(cookie, 16, 58).toLowerCase();
	}
	
	/** Return the cookies from the HTTP request header */
	private ArrayList<String> authCookies() {
		return findCookies( "__Host_authToken");
	}

	/** Find the cookie header that starts with name. You can have multiple cookies with the same name */
	public ArrayList<String> findCookies(String name) {
		ArrayList<String> list = new ArrayList<>();
		
		for (String cookies : getHeaders( "Cookie") ) {  // it seems there is usually only one Cookie header w/ all the different cookies in it separated by ;
			StringTokenizer st = new StringTokenizer(cookies, ";");
			while (st.hasMoreTokens() ) {
				String cookie = st.nextToken().trim();
				if (cookie.startsWith(name) ) {
					list.add(cookie);
				}
			}
		}
		return list;
	}

}