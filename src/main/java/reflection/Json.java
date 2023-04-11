package reflection;

import org.json.simple.JSONAware;

import json.MyJsonObject;

/** Consider StringJson instead of this class. */   // this class no longer serves any purpose
public class Json {						// and should be removed. pas

	private String m_str;

	public long length() { return m_str.length(); }
	public byte[] getBytes() { return m_str.getBytes(); }

	public Json(MyJsonObject obj) {
		m_str = obj.toString();
	}

	public Json(JSONAware obj) {
		m_str = obj.toJSONString();
	}
	
	@Override public String toString() {
		return m_str;
	}
	
	/** If this is an array of objects, it will put them one on each line for easier reading. */
	public Json fmtArray() {
		m_str = m_str.replaceAll( ",\\{", ",\n{");
		return this;
	}
}
