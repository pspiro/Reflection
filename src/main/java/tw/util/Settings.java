/**
 * 
 */
package tw.util;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map.Entry;




public class Settings {
	private final HashMap<String,String> m_map = new HashMap<String,String>();
	private final String m_filename = String.format( "%s\\%s", S.getTempDir(), "settings");
	
	public String get( String key) 				{ return m_map.get( key); }
	public void put( String key, String val) 	{ m_map.put( key, val); }
	
	public void read() {
		try {
			IStream is = new IStream( m_filename);
			String str;
			while( (str=is.readln() ) != null) {
				StringTokenizer st = new StringTokenizer( str, "=");
				m_map.put( st.nextToken(), st.nextToken() );
			}
		}
		catch( FileNotFoundException e) {
			// eat it
		}
	}
	
	public void write() {
		try {
			OStream os = new OStream( m_filename, false);
			for( Entry<String,String> entry : m_map.entrySet() ) {
				os.writeln( String.format( "%s=%s", entry.getKey(), entry.getValue() ) );
			}
			os.close();
		} 
		catch( Exception e) {
			e.printStackTrace();
		}
	}
}