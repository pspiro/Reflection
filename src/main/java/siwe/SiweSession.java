package siwe;

import org.json.simple.JsonObject;
import org.json.simple.STable;

import common.Util;

public class SiweSession implements STable.Ser {
	private String m_nonce;
	private long m_lastTime;
	
	/** Needed for deserialization */
	public SiweSession() {
	}

	public SiweSession(String nonce) {
		m_nonce = nonce;
		m_lastTime = System.currentTimeMillis();
	}

	public String nonce() {
		return m_nonce;
	}
	
	public long lastTime() {
		return m_lastTime;
	}

	public void update() {
		m_lastTime = System.currentTimeMillis();
	}
	
	@Override public String toString() {
		return m_nonce;
	}
	
	public JsonObject getJson() {
		return Util.toJson( "nonce", m_nonce, "time", m_lastTime);
	}
	
	public void setJson( JsonObject obj) {
		m_nonce = obj.getString( "nonce");
		m_lastTime = obj.getLong( "time");
	}
}