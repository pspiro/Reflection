package org.json.simple;

import java.util.HashMap;
import java.util.Map;

import common.Util;
import tw.util.S;

/** A "S"erialized table.
 * 
 *  Restores the map from a file. Writes the entire map to the file up to
 *  every n seconds
 *
 *  The objects stored in the map must either store native types or implement Ser
 *  
 *  Note that Integer doesn't work because the values are read back in as Long;
 *  you could fix it by reading them back in as Integer or by changing them
 *  to Integer after reading them; see JsonArray.convertToDouble() */ 
public class STable<T> extends HashMap<String,T> {
	public interface Ser {
		JsonObject getJson();  // can return a native type or json object
		void setJson( JsonObject obj);  // obj is a native type or json object
	}
		
	private String m_filename;
	private int m_period; // in ms
	private boolean m_set;
	
	public STable( String filename, int n, Class<T> clas) {
		m_filename = filename;
		m_period = n;
		
		readFile(clas);
		
		Util.executeEvery(m_period, m_period, () -> check() );
	}

	@Override public T put(String key, T value) {
		try {
			return (T)super.put(key, value);
		}
		finally {
			queue();
		}
	}
	
	@Override public T remove(Object key) {
		try {
			return (T)super.remove(key);
		}
		finally {
			queue();
		}
	}
	
	@Override public boolean remove(Object key, Object value) {
		try {
			return super.remove(key, value);
		}
		finally {
			queue();
		}
	}

	@Override public void putAll(Map<? extends String, ? extends T> other) {
		super.putAll(other);
		queue();
	}
	
	@Override public T putIfAbsent(String key, T value) {
		try {
			return super.putIfAbsent(key, value);
		}
		finally {
			queue();
		}
	}
	
	private synchronized void queue() {
	    m_set = true;
    }
	
	private synchronized void check() {
		if (m_set) {
			write();
			m_set = false;
		}
	}
	
	@Override public T get(Object key) {
		return (T)super.get(key);
	}

	private void write() {
		try {
			S.out( "Writing %s elements to %s", size(), m_filename);
			JsonObject json = new JsonObject();
			forEach( (key,val) -> json.put( key, getWritable( val) ) );  // val could be native object or Ser
			json.writeToFile( m_filename);
		}
		catch( Exception e) {
			S.err( "Error writing to DTable " + m_filename, e);
		}			
	}
	
	private Object getWritable(T val) {
		return val instanceof Ser ? ((Ser)val).getJson() : val;
	}

	private void readFile(Class<T> clas) {
		try {
			Util.require( clas != Integer.class, "Use Long not int; vals are deserialized as long");

			JsonObject json = JsonObject.readFromFile( m_filename);
			Util.forEach( json, (key,val) -> {
				if (val instanceof JsonObject) {
					T inst = clas.getDeclaredConstructor().newInstance();
					((Ser)inst).setJson( (JsonObject)val);  // val could be native object or JsonObject
					put( key, inst);
				}
				else {
					Util.require( val.getClass() == clas, "Error: cannot read file %s, a row has the wrong type", m_filename);
					put( key, (T)val);
				}
			});
			
			S.out( "Read %s elements from %s", size(), m_filename);
		}
		catch( Exception e) {
			S.err( "Error while reading DTable " + m_filename, e);
		}
	}
	
//	public static void main(String[] args) throws Exception {
//		STable<Long> stab = new STable<>( "c:/temp/f.t", 1000, Long.class);
//		
//		for (int i = 0; i < 3; i++) {
//			stab.put( "" + i, (long)i);
//			S.sleep(1000);
//		}
//		
//		STable<Long> stab2 = new STable<>( "c:/temp/f.t", 1000, Long.class);
//		stab2.forEach( (k,v) -> S.out( "%s %s", k, v) );
//
//	
//	
//		STable<Session> stab3 = new STable<>( "c:/temp/f1.t", 1000, Session.class);
//		
//		for (int i = 0; i < 3; i++) {
//			stab3.put( "" + i, new Session( "" + i) );
//			S.sleep(1000);
//		}
//		
//		STable<Session> stab4 = new STable<>( "c:/temp/f1.t", 1000, Session.class);
//		stab4.forEach( (k,v) -> S.out( "%s %s", k, v) );
//	}

}
