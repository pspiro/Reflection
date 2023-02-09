package tw.google;

import java.util.HashMap;

import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;


/** A hashtable backed by a google sheet with two columns. */
public class GTable extends HashMap<String,String> {
	private Tab m_tab;
	private String m_col1;
	private String m_col2;
	private boolean m_caseSensitive; // applies to the tags
	
	public GTable() throws Exception {
	}

	/** @param lowerCase if set to true, tags will be converted to lower case 
	 *         get() could be made to use it as well */
	public GTable( String sheetId, String tabName, String col1, String col2) throws Exception {
		this( sheetId, tabName, col1, col2, true);
	}
	
	public GTable( String sheetId, String tabName, String col1, String col2, boolean caseSensitive) throws Exception {
		m_col1 = col1;
		m_col2 = col2;
		m_caseSensitive = caseSensitive;
		
		m_tab = NewSheet.getTab( sheetId, tabName);
		
		// build map
		for ( ListEntry row : m_tab.fetchRows() ) {
			String tag = row.getValue( m_col1);
			String val = row.getValue( m_col2);
			if (tag != null && val != null) {
				super.put( m_caseSensitive ? tag : tag.toLowerCase(), val);
			}
		}
	}
	
	@Override public String get(Object key) {
		return super.get( m_caseSensitive ? key : ((String)key).toLowerCase() );
	}

	/** true for true or yes, everything else false */
	public boolean getBoolean(String key) {
		String val = S.notNull( get(key) ).toLowerCase();
		return val.equals("true") || val.equals("yes");
	}
	
	public String getRequiredString(Object key) throws Exception {
		String val = get(key);
		if (S.isNull(val) ) {
			throw new Exception(String.format("Missing required key '%s' from google sheet", key) );
		}
		return val;
	}
	
	public double getDouble(String tag) throws Exception {
		try {
			return Double.valueOf( get( tag) );
		}
		catch( Exception e) {
			throw new Exception( String.format( "Tag %s is not a number", tag) );
		}
	}
	
	public int getInt(String tag) throws Exception {
		try {
			return Integer.valueOf( get( tag) );
		}
		catch( Exception e) {
			throw new Exception( String.format( "Tag %s is not an integer", tag) );
		}
	}
	
	// update map and sheet
	@Override public String put(String tag, String newVal) {
		try { 
			String curVal = get( tag);
			
			// if tag not found, add new row
			if (curVal == null) {
				ListEntry row = m_tab.newListEntry();
				row.setValue( m_col1, tag);
				row.setValue( m_col2, newVal);
				row.insert();
			}
			
			// if value has changed, updated existing row
			else if (!newVal.equals( curVal) ) {
				for ( ListEntry row : m_tab.fetchRows() ) {
					if (tag.equals( row.getValue( m_col1) ) ) {
						row.setValue(m_col2, newVal);
						row.update();
					}
				}
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return super.put( tag, newVal);
	}

	public static void main(String[] args) {
		try {
			GTable table = new GTable( "Structure", "Test", "Tag", "Val");
			table.put( "a", "bb");
			table.put( "b", "c");
			table.put( "d", "e");
			table.put( "f", "gg");
			table.put( "h", "i");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getName(String name) {
		String val = get( name);
		return val != null ? val : name;
	}
}
