package json;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import tw.util.S;

/** Array of object only. */
public class MyJsonArray implements Iterable<MyJsonObject> { 
	private JSONArray m_ar;
	
	public static void main(String[] args) throws Exception {
		MyJsonObject.parse( "{ \"a\":[ 4,5,6], \"b\": { \"c\": 7 } }").display();
		S.out( "-----");
		MyJsonArray.parse( "[ 4,[5,6],7,{\"a\":8,\"b\":9},{\"a\":8,\"b\":9}]").display();
	}
	
	public static MyJsonArray parse( String text) throws ParseException {
		return new MyJsonArray( new JSONParser().parse( text) );
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

	public static boolean isArray(String ret) {
		return S.isNotNull(ret) && ret.startsWith("[");
	}
	
	public int size() {
		return m_ar.size();
	}
}
