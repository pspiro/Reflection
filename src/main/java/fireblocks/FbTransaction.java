package fireblocks;

import org.json.simple.JsonArray;
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
					"mapSize", FbServer.m_map.size(),
					"lastSuccessfulFetch", FbServer.m_lastSuccessfulFetch,
					"lastSuccessfulPut", FbServer.m_lastSuccessfulPut
			);
			respond( obj);
		});
	}

	/** Called by Monitor */
	public void onGetAll() {
		wrap( () -> {
			JsonArray recs = new JsonArray();
			FbServer.m_map.values().forEach( trans -> {
				JsonObject obj = Util.toJson( 
						"id", trans.id(),
						"status", trans.status(),
						"createdAt", trans.createdAt() );
				recs.add(obj);
			});

			recs.sort( (o1, o2) -> FbServer.comp( o1.getLong("createdAt"), o2.getLong("createdAt") ) );
			respond(recs);
		});
	}
}
