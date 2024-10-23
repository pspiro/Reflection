package test;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyServer;
import positions.AlchemyStreamMgr;
import positions.HookConfig;
import reflection.Config;
import tw.util.S;

public class TestStream {
	public static void main(String[] args) throws Exception {
		HookConfig c = new HookConfig();
		c.readFromSpreadsheet( Config.getTabName(args) );

		MyServer.listen( c.hookServerPort(), 10, server -> {
			server.createContext("/test/hook/webhook", exch -> new Trans(exch, false).handleWebhook() );
		});

//		MoralisStreams.createStream(
//				MoralisStreams.approval, 
//				"junk1", 
//				c.hookServerUrlBase() + "/hook/webhook", 
//				Util.toHex( c.chainId() ),
//				c.rusdAddr() );
		AlchemyStreamMgr mgr = new AlchemyStreamMgr( c.alchemyChain(), c.nativeTokName(), c.busdAddr() );
		mgr.createApprovalStream( Util.getNgrokUrl() + "/test", c.rusdAddr() );
		
		Util.executeIn( 5000, () -> {
			try {
				c.busd().approve( c.ownerKey(), c.rusdAddr(), 5).waitForHash();
				c.rusd().approve( c.ownerKey(), c.busdAddr(), 6).waitForHash();
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
