package http;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class PwServer {
	public static void main(String[] args) throws Exception {
		// make sure it will parse w/ no error but don't keep it in memory
		JsonObject.parse( System.getenv("json") );

		int port = Integer.valueOf( System.getenv("PORT") );
		S.out( "listening on port " + port);
		
		MyServer.listen( port, 1, server -> {
			server.createContext("/getpw", exch -> handle( new BaseTransaction(exch, true) ) );
		});
	}
	
	static void handle(BaseTransaction trans) {
		try {
			var received = trans.parseToObject();

			// validate name and code
			JsonObject pack = JsonObject.parse( System.getenv("json") )
					.getObject( received.getString( "name") );
			Util.require( pack != null, "Invalid name");
			Util.require( received.getString( "code").equals( pack.getString( "code")), "Invalid code" );

			// validate source IP address
			String sourceIp = trans.exchange().getRemoteAddress().getAddress().getHostAddress();
			Util.require( S.isNotNull( sourceIp), "Could not get source IP address");
			Util.require( isValid( sourceIp, pack.getString( "ips") ), "Invalid source IP '%s'", sourceIp); 

			// respond with password
			trans.respond( Util.toJson( "pw", pack.getString( "pw")) );
		}
		catch (Exception e) {
			e.printStackTrace();
			trans.respond( Util.toJson( "error", e.getMessage() ) );
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
