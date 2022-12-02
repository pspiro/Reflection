package json;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import reflection.Util;
import tw.util.S;

/** Use MyJsonObj when you are reading or parsing; use TypedJson when you are creating */ 
public class MyJsonObject {  // replace or combine w/ TypedJson
	
	private JSONObject m_obj;
	
	public MyJsonObject( Object obj) {
		m_obj = (JSONObject)obj;
	}
	
	public static MyJsonObject parse( String text) throws ParseException {
		return new MyJsonObject( new JSONParser().parse( text) );
	}

	public MyJsonArray getAr(String key) {
		return new MyJsonArray( m_obj.get( key) );
	}

	public MyJsonObject getObj(String key) {
		return new MyJsonObject( m_obj.get( key) );
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

	public void display() {
		display( m_obj, 0, false);
	}
	
//	@Override public String toString() {
//		return super.toString();
//	}
	
	public static void display(Object objIn, int level, boolean force) {
		if (objIn instanceof JSONObject) {
			if (force) {
				System.out.print( Util.tab( level) );
			}
			System.out.println( "{");

			JSONObject map = ((JSONObject)objIn);
			
			for (Object key : map.keySet() ) {
				Object val = map.get( key);
				System.out.print( String.format( "%s%s : ", Util.tab( level+1), key) );
				display( val, level + 1, false);
				System.out.println( ", ");  // leaves an extra , on the last one, not good
			}
			System.out.print( Util.tab( level) + "}, ");
		}
		else if (objIn instanceof JSONArray) {
			System.out.println( "[");
			
			JSONArray ar = (JSONArray)objIn;
			for (Object obj : ar) {
				display( obj, level + 1, true);
			}
			System.out.print( Util.tab(level) + "]");			
		}
		else {
			if (force) {
				System.out.println( String.format( "%s%s,", Util.tab( level), objIn) );				
			}
			else {
				System.out.print( objIn);
			}
		}
		
	}

}