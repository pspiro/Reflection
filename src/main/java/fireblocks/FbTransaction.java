package fireblocks;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;

public class FbTransaction extends BaseTransaction {

	public FbTransaction(HttpExchange exchange) {
		super(exchange);
	}

	public void onOk() {
		wrap( () -> respondOk() );
	}

	public void onStatus() {
		wrap( () -> {
			JsonObject obj = Util.toJson(
					"code", "OK",
					"started", FbActiveServer.m_started,
					"mapSize", FbActiveServer.m_map.size()
			);
			respond( obj);
		});
	}

	public void onDebug(boolean b) {
		wrap( () -> {
			FbActiveServer.m_debug = b;
			respondOk();
		});
	}
	
}
