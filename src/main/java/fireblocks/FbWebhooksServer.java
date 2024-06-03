package fireblocks;

import java.util.Date;

import org.json.simple.JsonObject;

import http.SimpleTransaction;
import tw.util.S;

/** Fireblocks Webhooks server listens for events from Fireblocs;
 *  we never used this, we used polling instead because we got the results faster */
public class FbWebhooksServer {
	public static void main(String[] args) {
		String host = "0.0.0.0";
		int port = Integer.valueOf( args[0]);
		
		S.out( "listening on %s:%s", host, port);
		
		SimpleTransaction.listen( host, port, simpleTrans -> {
			try {
				JsonObject json = simpleTrans.getJson();
				long t = json.getLong("lastUpdated");
				S.out( "%s %s %s %s",
						new Date(t),
						json.getString("type"), 
						json.getObject("data").getString("status"),
						json.getObject("data").getString("subStatus")
					);
				
				simpleTrans.respond( "ok");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
