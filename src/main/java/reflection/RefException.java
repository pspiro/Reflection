package reflection;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class RefException extends Exception {
	private RefCode m_code;

	public RefCode code() { return m_code; }

	public RefException( RefCode code, String first, Object... params) {  // my text is the detail message
		super( String.format( S.notNull( first), params) );
		m_code = code;
	}
	
	@Override public String toString() {
		return m_code + " " + getMessage();
	}

	public JsonObject toJson() {
		return eToJson(this, m_code);
	}
	
	public static JsonObject eToJson(Exception e) {
		return eToJson(e, RefCode.UNKNOWN);
	}
	
	public static JsonObject eToJson(Exception e, RefCode refCode) {
		return Util.toJson(
				"code", refCode, 
				"message", e.getMessage() != null ? e.getMessage() : e.toString(),
				"statusCode", 400
			);
	}

}
