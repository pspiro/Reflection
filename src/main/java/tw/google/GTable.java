package tw.google;

import java.util.HashMap;

import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;


/** A hashtable backed by a google sheet with two columns. */
public class GTable extends HashMap<String,String> {
	private Tab m_tab;
	private String m_col1;
	private String m_col2;
	
	public GTable( String sheetId, String tabName, String col1, String col2) throws Exception {
		m_col1 = col1;
		m_col2 = col2;
		
		m_tab = NewSheet.getTab( sheetId, tabName);
		
		// build map
		for ( ListEntry row : m_tab.fetchRows() ) {
			String tag = row.getValue( m_col1);
			String val = row.getValue( m_col2);
			if (tag != null && val != null) {
				super.put( tag, val);
			}
		}
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
			throw new Exception( String.format( "Tag %s is not a number", tag) );
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
