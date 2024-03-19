package positions;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.Accounts;
import fireblocks.MyServer;
import http.BaseTransaction;
import http.MyClient;
import reflection.Config;
import reflection.RefCode;
import tw.util.S;

// bug: when we received 2, we updated to 2 instead of add
// issue: how to tell if the updates are old or new; you don't
// want to play back old updates

public class TestAppr {
	final Config m_config = new Config();
	
	public static void main(String[] args) {
		try {
			new TestAppr().run("Prod-config");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}
	
	void run(String tabName) throws Exception {
		MyClient.filename = "hookserver.http.log";
		m_config.readFromSpreadsheet(tabName);
		BaseTransaction.setDebug( true);
		
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch, true).handleWebhook() );
			server.createContext("/", exch -> new Trans(exch, false).noMatch() );
		});

		// listen for "approve" transactions
		Streams.createStream(
						Streams.approval, 
						"test", 
						"http://108.6.23.121/hook/webhook", 
						"0x5",
						Accounts.instance.getAddress("Owner") );
				//m_config.rusd().address() );

		S.out( "**ready**");
	}

	class Trans extends BaseTransaction {
		
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}

		public void noMatch() {
			respond( Util.toJson( code, RefCode.OK, "message", "No matching endpoint") );
		}

		void handleWebhook() {
			wrap( () -> {
				if (m_exchange.getRequestBody().available() == 0) {
					S.out( "  (no data)");
				}
				else {
					handleHookWithData();
				}
				respondOk();
			});
		}
		
		private void handleHookWithData() throws Exception {
			JsonObject obj = parseToObject();
			String tag = obj.getString("tag");
			boolean confirmed = obj.getBool("confirmed");
			S.out( "Received hook [%s - %s] %s", tag, confirmed, obj);  // change this to debug mode only
		}
	}
}