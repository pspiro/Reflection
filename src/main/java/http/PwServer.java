package http;

import java.io.IOException;

import common.Util;
import tw.util.S;

public class PwServer {
	public static void main(String[] args) throws IOException {
		int port = Integer.valueOf( System.getenv("PORT") );
		String code = System.getenv("code");
		
		S.out( "listening on port " + port);
		
		MyServer.listen( port, 1, server -> {
			server.createContext("/getpw", exch -> {
				BaseTransaction trans = new BaseTransaction(exch, true);
				try {
					var obj = trans.parseToObject();
					String ret = obj.getString( "code").equals( code) ? System.getenv("pw") : "wrong code";
					trans.respond( Util.toJson( "pw", ret) );
				} catch (Exception e) {
					e.printStackTrace();
					trans.respondOk();
				}
			});
		});
		
	}
}
