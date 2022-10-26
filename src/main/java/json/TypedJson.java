package json;

import org.json.simple.JSONObject;

/** Use MyJsonObj when you are reading or parsing; use TypedJson when you are creating */
public class TypedJson<T> extends JSONObject {
	@Override public T get(Object key) {
		return (T)super.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public void putt( String tag, T value) {
		put( tag, value);
	}
}