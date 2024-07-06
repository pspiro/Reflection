package testcase;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyServer;
import positions.Streams;
import reflection.Config;
import tw.util.S;

public class TestStream {
	public static void main(String[] args) throws Exception {
		Config c = Config.read();

		MyServer.listen( c.hookServerPort(), 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch, false).handleWebhook() );
		});

		Streams.createStream(
				Streams.approval, 
				"junk1", 
				c.hookServerUrl(), 
				Util.toHex( c.chainId() ),
				c.rusdAddr() );

		Util.executeIn( 5000, () -> {
			try {
				c.busd().approve( c.ownerKey(), c.rusdAddr(), 5);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	static class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
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

			S.out( "Received hook [%s - %s] %s", tag, confirmed, obj);
		}
	}
}
