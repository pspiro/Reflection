package redis;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import util.LogType;

public class MdTransaction extends BaseTransaction {
	// copy
//	wrap( () -> {
//	});

	private MktDataServer m_main;
	
	MdTransaction(MktDataServer main, HttpExchange exchange) {
		super(exchange);
		m_main = main;
	}
	
	public void onOk() {
		wrap( () -> {
			JsonObject obj = Util.toJson(
					"code", "OK",
					"TWS", m_main.m_mdConnMgr.isConnected(),
					"IB", m_main.m_mdConnMgr.ibConnection() );
			respond( obj);
		});
	}

	public void onStatus() {
		wrap( () -> {
		});
	}
	
	@Override protected void jlog(LogType type, JsonObject json) {
		super.jlog(type, json);
	}
}
