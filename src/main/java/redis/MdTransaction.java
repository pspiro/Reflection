package redis;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import util.LogType;

public class MdTransaction extends BaseTransaction {
	/*
	wrap( () -> {
	});
*/

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
					"IB", m_main.m_mdConnMgr.ibConnection(),
					"mdCount", m_main.mdController().mdCount() 
					);
			respond( obj);
		});
	}

	@Override protected void jlog(LogType type, JsonObject json) {
		super.jlog(type, json);
	}

	public void onDesubscribe() {
		wrap( () -> {
			m_main.desubscribe();
			respondOk();
		});
	}

	public void onSubscribe() {
		wrap( () -> {
			m_main.subscribe();
			respondOk();
		});
	}

	public void onDisconnect() {
		wrap( () -> {
			m_main.mdConnMgr().disconnect();
			respondOk();
		});
	}

	public void onGetPrices() {
		wrap( () -> {
			respond( m_main.getAllPrices() );
		});
	}
}
