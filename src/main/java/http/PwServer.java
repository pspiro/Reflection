package http;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

/** This program runs in a google cloud run and retrieves the password from a google secret.
 *  The password is to read the blockchain private keys */
public class PwServer {
	public static void main(String[] args) throws Exception {
		// make sure it will parse w/ no error but don't keep it in memory
		JsonObject.parse( System.getenv("json") );

		int port = Integer.valueOf( System.getenv("PORT") );

		MyServer.listen( port, 1, server -> {
			server.createContext("/", exch -> handleCatchAll( new BaseTransaction(exch, true) ) );
			server.createContext("/ok", exch -> new BaseTransaction(exch, true).respondOk() );
			server.createContext("/getpw", exch -> handleGetPw( new BaseTransaction(exch, true) ) );
		});
	}

	private static void handleCatchAll(BaseTransaction trans) {
		trans.respond( Util.toJson( "error", "No such endpoint") );
	}

	static void handleGetPw(BaseTransaction trans) {
		try {
			// read the request from client
			var request = trans.parseToObject();

			// read the list of codes from the google 'secretes'
			JsonObject codes = JsonObject.parse( System.getenv("json") );

			// get the 'pack' (code+pw+ips)
			var pack = codes.getObject( request.getString( "name") );
			Util.require( pack != null, "Invalid name");
			Util.require( request.getString( "code").equals( pack.getString( "code")), "Invalid code" );

			// validate source IP address
			String sourceIp = trans.exchange().getRemoteAddress().getAddress().getHostAddress();
			Util.require( S.isNotNull( sourceIp), "Could not get source IP address");
			Util.require( isValid( sourceIp, pack.getString( "ips") ), "Invalid source IP '%s'", sourceIp);

			// respond with password
			trans.respond( Util.toJson( "pw", pack.getString( "pw")) );
		}
		catch (Exception e) {
			e.printStackTrace();
			trans.respond( Util.toJson( "error", e.toString() ) );
		}
	}

	private static boolean isValid(String remote, String ips) {
		for ( var ip : ips.split( " ") ) {
			if (remote.equals( ip) ) {
				return true;
			}
		}
		return false;
	}

}
