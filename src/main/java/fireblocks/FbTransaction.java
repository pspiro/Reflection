package fireblocks;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;

public class FbTransaction extends BaseTransaction {

	public FbTransaction(HttpExchange exchange) {
		super(exchange, true);
	}

	public void onStatus() {
		wrap( () -> {
			JsonObject obj = Util.toJson(
					"code", "OK",
					"started", FbServer.m_started,
					"mapSize", FbServer.m_map.size()
			);
			respond( obj);
		});
	}
}
