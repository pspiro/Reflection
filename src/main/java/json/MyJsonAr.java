package json;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Array of object only. */
public class MyJsonAr implements Iterable<MyJsonObj> { 
	private JSONArray m_ar;
	
	public static MyJsonAr parse( String text) throws ParseException {
		return new MyJsonAr( new JSONParser().parse( text) );
	}

	MyJsonAr( Object obj) {
		m_ar = (JSONArray)obj;
	}
	
	@Override public String toString() {  // this class sucks. pas
		return m_ar.toString();
	}
	
	public MyJsonObj getJsonObj( int i) {
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