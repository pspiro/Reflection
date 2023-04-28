package json;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import reflection.Util;
import tw.util.S;

/** Array of object only. */
public class MyJsonArray implements Iterable<MyJsonObject> { 
	private JSONArray m_ar;
	
	public static MyJsonArray parse( String text) throws Exception {
		Util.require( isArray(text), "Error: not a json array: " + text);
		return new MyJsonArray( new JSONParser().parse( text) );
	}
	
	public JSONArray getArray() {
		return m_ar;
	}
	
	public int size() {
		return m_ar.size();
	}

	public MyJsonArray( Object obj) {
		m_ar = (JSONArray)obj;
	}
	
	@Override public String toString() {  // this class sucks. pas
		return m_ar.toString();
	}
	
	public MyJsonObject getJsonObj( int i) throws Exception {
		return new MyJsonObject( m_ar.get( i) );
	}

	@Override public Iterator<MyJsonObject> iterator() {
		return new Iterator<MyJsonObject>() {
			Iterator<Object> iter = m_ar.iterator();
			
			@Override public boolean hasNext() {
				return iter.hasNext();
			}

			@Override public MyJsonObject next() {
				try {
					Object obj = iter.next();
					return obj != null ? new MyJsonObject(obj) : null;
				}
				catch( NullPointerException e) {
					e.printStackTrace();  
					return null;  // this will never happen
				}
			}
		};
	}
	
	public void display() {
		MyJsonObject.display(m_ar, 0, false);
	}

	public static boolean isArray(String ret) {
		return ret != null && ret.trim().startsWith("[");
	}
}
