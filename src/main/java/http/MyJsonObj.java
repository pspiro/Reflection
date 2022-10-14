package http;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import reflection.Util;
import tw.util.S;

/** Use MyJsonObj when you are reading or parsing; use TypedJson when you are creating */ 
public class MyJsonObj {  // replace or combine w/ TypedJson
	
	/** Array of object only. */
	public static class MyJsonAr implements Iterable<MyJsonObj> { 
		private JSONArray m_ar;
		
		MyJsonAr( Object obj) {
			m_ar = (JSONArray)obj;
		}
		
		MyJsonObj get( int i) {
			return new MyJsonObj( m_ar.get( i) );
		}

		@Override public Iterator<MyJsonObj> iterator() {
			return new Iterator<MyJsonObj>() {
				Iterator<Object> iter = m_ar.iterator();
				
				@Override public boolean hasNext() {
					return iter.hasNext();
				}

				@Override public MyJsonObj next() {
					return new MyJsonObj( iter.next() );
				}
			};
		}
	}

	
	
	private JSONObject m_obj;
	
	MyJsonObj( Object obj) {
		m_obj = (JSONObject)obj;
	}
	
	public static MyJsonObj parse( String text) throws ParseException {
		return new MyJsonObj( new JSONParser().parse( text) );
	}

	public MyJsonAr getAr(String key) {
		return new MyJsonAr( m_obj.get( key) );
	}

	public MyJsonObj getObj(String key) {
		return new MyJsonObj( m_obj.get( key) );
	}
	
	public String getString(String key) {
		return (String)m_obj.get( key);
	}
	
	/** Returns zero for null value. */
	public int getInt( String key) {
		String str = getString( key);
		return S.isNotNull( str) ? Integer.parseInt( str) : 0;
	}

	public void display() {
		display( m_obj, 0);
	}

	public double getDouble(String key) {
		return Double.valueOf( getString( key) );
	}
	
//	@Override public String toString() {
//		return super.toString();
//	}
	
	public static void display(Object objIn, int level) {
		if (objIn instanceof JSONObject) {
			System.out.println( "{");

			JSONObject map = ((JSONObject)objIn);
			
			for (Object key : map.keySet() ) {
				Object val = map.get( key);
				System.out.print( String.format( "%s%s : ", Util.tab( level), key) );
				display( val, level + 1);
				System.out.println( "");
			}
			System.out.println( Util.tab( level) + "}");
		}
		else if (objIn instanceof JSONArray) {
			System.out.println( "[ ");
			JSONArray ar = (JSONArray)objIn;
			for (Object obj : ar) {
				display( obj, level + 1);
			}
			System.out.print( " ] ");			
		}
		else {
			System.out.print( objIn);
		}
		
	}

	
}