package reflection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** @deprecated use TypedJson */
public class Json {

	private String m_str;

	public long length() { return m_str.length(); }
	public byte[] getBytes() { return m_str.getBytes(); }

	public Json(String string) {
		m_str = string;
	}

	public Json(JSONArray ar) {
		m_str = ar.toJSONString();
	}
	
	public Json(JSONObject obj) {
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
