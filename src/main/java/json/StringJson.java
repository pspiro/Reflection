package json;

import org.json.simple.JSONObject;

/** This is a Json object where are the values are Strings.
 *  Use MyJsonObj when you are reading or parsing; use this when you are creating */
public class StringJson extends JSONObject {
	@Override public String get(Object key) {
		return (String)super.get(key);
	}
	
//	public static class StringJsonArray extends JSONArray {
//		@Override public String get(int index) {
//			return (String)super.get(index);
//		}
//	}

	public int getInt(String tag) {
		return Integer.valueOf( get(tag) );
	}
}
