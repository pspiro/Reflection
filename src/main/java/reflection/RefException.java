package reflection;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

public class RefException extends Exception {
	private RefCode m_code;  // part of toString() but not getMessage()

	public RefCode code() { return m_code; }

	public RefException( RefCode code, String first, Object... params) {  // my text is the detail message
		super( S.format( S.notNull( first), params) );  // by using S.format we can avoid some errors when the format string contains %
		m_code = code;
	}
	
	@Override public String toString() {
		return m_code + " " + getMessage();
	}

	public JsonObject toJson() {
		return eToJson(this, m_code);
	}
	
	public static JsonObject eToJson(Throwable e) {
		return eToJson(e, RefCode.UNKNOWN);
	}

	public static void main(String[] args) {
		eToJson( new Exception("bad"), RefCode.TOO_SLOW).display();
	}

	public static JsonObject eToJson(Throwable e, RefCode refCode) {
		String text = Util.toMsg(e);
		
		return Util.toJson(
				"code", refCode, 
				"message", text,
				"error", Util.toJson("message", text),
				"statusCode", 400
			);
	}

}
