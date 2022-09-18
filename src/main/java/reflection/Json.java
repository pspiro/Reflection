package reflection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Json {

	private final String m_str;

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
	
	public String getLog() {
		return Util.flatten( m_str);
	}
}
