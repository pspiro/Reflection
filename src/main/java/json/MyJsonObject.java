package json;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.moonstoneid.siwe.SiweMessage;

import reflection.SiweUtil;
import reflection.Util;
import tw.util.S;

/** Use MyJsonObj when you are reading or parsing; use StringJson when you are creating.
 *  The only current limitation is that getAr() assumes that you get an array of object
 *  which is not guaranteed */ 
public class MyJsonObject {  // replace or combine w/ TypedJson
	
	private JSONObject m_obj;
	
	public MyJsonObject( Object obj) throws NullPointerException {  // one of the rare places we return NPE due to the code in MyJsonArray.iter
		if (obj == null) throw new NullPointerException("Cannot create MyJsonObject from null JSONObject");
		m_obj = (JSONObject)obj;
	}
	
	public static MyJsonObject parse( String text) throws Exception {
		Util.require( isObject(text), "Error: not a json object: " + text);
		return new MyJsonObject( new JSONParser().parse( text) );
	}
	
	public static boolean isObject(String text) {
		return text != null && text.trim().startsWith("{");
	}

	public MyJsonArray getAr(String key) {
		return new MyJsonArray( m_obj.get( key) );
	}

	public MyJsonObject getObj(String key) throws Exception {
		Object obj = m_obj.get(key);
		return obj != null ? new MyJsonObject(obj) : null;
	}
	
	public JSONObject toJsonObj() {
		return m_obj;
	}
	
	/** Converts any object type to string or returns empty string, never null. */
	public String getString(String key) {
		Object val = m_obj.get(key);
		return val != null ? val.toString() : ""; 
	}
	
	/** Returns zero for null value. */
	public int getInt( String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Integer.parseInt( str) : 0;
	}

	public double getDouble(String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Double.valueOf( str) : 0.;
	}
	
	@Override public String toString() {
		return m_obj.toString();
	}

	public void display(String title) {
		S.out(title);
		display( m_obj, 0, false);
		System.out.println();
	}

	public void display() {
		display( m_obj, 0, false);
		System.out.println();
	}
	
//	@Override public String toString() {
//		return super.toString();
//	}
	
	public static void display(Object objIn, int level, boolean arrayItem) {
		if (objIn instanceof JSONObject) {
//			if (arrayItem) {
//				out( Util.tab( level) );
//			}
			out( "{\n");

			JSONObject map = ((JSONObject)objIn);
			
			boolean first = true;
			for (Object key : map.keySet() ) {
				Object val = map.get( key);
				
				if (val != null && val.toString().length() > 0) {
					if (!first) {
						out( ",\n");
					}

					out( "%s%s : ", Util.tab( level+1), key);
					display( val, level + 1, false);
					first = false;
				}
			}
			out( "\n%s%s", Util.tab(level), "}");
		}
		else if (objIn instanceof JSONArray) {
			JSONArray ar = (JSONArray)objIn;
			
			if (ar.size() == 0) {
				out( "[ ]");
			}
			else {
				out( "[\n%s", Util.tab(level+1) );
				
				boolean first = true;
				for (Object obj : ar) {
					if (!first) {
						out( ", ");
					}
					display( obj, level + 1, true);
					first = false;
				}
				out( "\n%s]", Util.tab(level) );
			}
		}
		else {
			out( objIn);
		}
		
	}
	
	static void out( String format, Object... params) {
		System.out.print( String.format( format, params) );
	}

	static void out( Object str) {
		System.out.print( str);
	}

	public boolean getBool(String key) {
		return Boolean.parseBoolean( getString(key) );
	}

	public SiweMessage getSiweMessage() throws Exception {
		return SiweUtil.toSiweMessage(this);
	}

	public MyJsonObject getRequiredObj(String key) throws Exception {
		MyJsonObject obj = getObj(key);
		Util.require(obj != null, "The required key is missing from the json object: " + key);
		return obj;
	}
}
