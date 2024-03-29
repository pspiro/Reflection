package org.json.simple;

import java.util.Map;

import common.Util;
import tw.util.S;

/** A "S"erialized table.
 * 
 *  Restores the map from a file. Writes the entire map to the file up to
 *  every n seconds */ 
public class STable<T> extends JsonObject {
	private String m_filename;
	private int m_period; // in ms
	private boolean m_set;
	
	public STable( String filename, int n) {
		m_filename = filename;
		m_period = n;
		
		try {
			putAll( JsonObject.readFromFile( filename) );
			S.out( "Read %s elements from %s", size(), filename);
		}
		catch( Exception e) {
			S.err( "Error while reading DTable " + filename, e);
		}
		
		Util.executeEvery(m_period, m_period, () -> check() );
	}

	@SuppressWarnings("unchecked")
	@Override public T put(String key, Object value) {
		try {
			return (T)super.put(key, value);
		}
		finally {
			queue();
		}
	}
	
	@SuppressWarnings("unchecked")
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
	
	@Override public void putAll(Map<? extends String, ? extends Object> m) {
		super.putAll(m);
		queue();
	}
	
	@Override public Object putIfAbsent(String key, Object value) {
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
	
	@SuppressWarnings("unchecked")
	@Override public T get(Object key) {
		return (T)super.get(key);
	}

	private void write() {
		try {
			S.out( "Writing %s elements to %s", size(), m_filename);
			writeToFile( m_filename);
		}
		catch( Exception e) {
			S.err( "Error writing to DTable " + m_filename, e);
		}			
	}
	

	public static void main(String[] args) throws Exception {
		STable<Integer> d = new STable<>( "c:/temp/f.t", 3000);
		
		for (int i = 0; i < 15; i++) {
			d.remove( "" + i);
			S.sleep(1000);
		}
		
	}

}
