package http;

import java.util.Date;

import org.json.simple.JsonObject;

import tw.util.S;

/** Fireblocks Webhooks server */
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
