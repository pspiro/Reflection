package json;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Array of object only. */
public class MyJsonArray implements Iterable<MyJsonObject> { 
	private JSONArray m_ar;
	
	public static void main(String[] args) throws ParseException {
		MyJsonObject.parse( "{ \"a\":[ 4,5,6] }")
			.display();
	}
	
	public static MyJsonArray parse( String text) throws ParseException {
		return new MyJsonArray( new JSONParser().parse( text) );
	}

	public MyJsonArray( Object obj) {
		m_ar = (JSONArray)obj;
	}
	
	@Override public String toString() {  // this class sucks. pas
		return m_ar.toString();
	}
	
	public MyJsonObject getJsonObj( int i) {
		return new MyJsonObject( m_ar.get( i) );
	}

	@Override public Iterator<MyJsonObject> iterator() {
		return new Iterator<MyJsonObject>() {
			Iterator<Object> iter = m_ar.iterator();
			
			@Override public boolean hasNext() {
				return iter.hasNext();
			}

			@Override public MyJsonObject next() {
				return new MyJsonObject( iter.next() );
			}
		};
	}
	
	public void display() {
		MyJsonObject.display(m_ar, 0, false);
	}
}
