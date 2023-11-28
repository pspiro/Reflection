package redis;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import util.LogType;

public class MdTransaction extends BaseTransaction {
	private MdServer m_main;
	
	MdTransaction(MdServer main, HttpExchange exchange) {
		this( main, exchange, true);
	}
	
	MdTransaction(MdServer main, HttpExchange exchange, boolean debug) {
		super(exchange, debug);
		m_main = main;
	}
	
	public void onStatus() {
		wrap( () -> {
			JsonObject obj = Util.toJson(
					"code", "OK",
					"TWS", m_main.m_mdConnMgr.isConnected(),
					"IB", m_main.m_mdConnMgr.ibConnection(),
					"mdCount", m_main.mdController().mdCount(),
					"started", m_main.m_started
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

	/** Called by monitor; returns all prices */
	public void onGetAllPrices() {
		wrap( () -> {
			respond( m_main.getAllPrices() );
		});
	}

	/** Called by RefAPI; returns current prices */
	public void onGetRefPrices() {
		wrap( () -> {
			respond( m_main.getRefPrices() );
		});
	}
}
