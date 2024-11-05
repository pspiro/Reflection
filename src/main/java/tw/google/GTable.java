package tw.google;

import java.util.HashMap;

import org.json.simple.JsonObject;

import common.Util.Ex;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;


/** A hashtable backed by a google sheet with two columns. */
public class GTable extends HashMap<String,String> {
	private Tab m_tab;
	private String m_col1;
	private String m_col2;  // can be null, in which case the map functions like a HashSet
	private boolean m_caseSensitive; // applies to the tags
	
	public String tabName() { return m_tab.name(); }
	
	public GTable() throws Exception {
	}

	/** @param col2 may be null; then is functions like a set */
	public GTable( String sheetId, String tabName, String col1, String col2) throws Exception {
		this( sheetId, tabName, col1, col2, true);
	}
	
	/** @param col2 may be null
	 *  @param caseSensitive applies to keys */
	public GTable( String sheetId, String tabName, String col1, String col2, boolean caseSensitive) throws Exception {
		this( NewSheet.getTab( sheetId, tabName), col1, col2, caseSensitive);
	}
	
	/** @param col2 may be null
	 *  @param caseSensitive applies to keys */
	public GTable( Tab tab, String col1, String col2, boolean caseSensitive) throws Exception {
		m_col1 = col1;
		m_col2 = col2;
		m_caseSensitive = caseSensitive;
		
		m_tab = tab;
		
		// build map
		for ( ListEntry row : m_tab.fetchRows() ) {
			String tag = row.getString( m_col1);
			String val = S.isNotNull( m_col2) ? row.getString( m_col2) : "";
			if (S.isNotNull( tag) && val != null) {
				super.put( m_caseSensitive ? tag : tag.toLowerCase(), val);
			}
		}
	}
	
	@Override public String get(Object key) {
		return super.get( m_caseSensitive ? key : ((String)key).toLowerCase() );
	}
	
	@Override public boolean containsKey(Object key) {
		return super.containsKey( m_caseSensitive ? key : ((String)key).toLowerCase() );
	}

	public String getNN(Object key) {
		return S.notNull(get(key));  
	}

	/** true for true or yes, everything else false */
	public boolean getBoolean(String key) {
		String val = S.notNull( get(key) ).toLowerCase();
		return val.equals("true") || val.equals("yes");
	}
	
	public String getRequiredString(Object key) throws Exception {
		String val = get(key);
		if (S.isNull(val) ) {
			throw new Ex("Missing required key '%s' from google sheet", key);
		}
		return val;
	}
	
	public double getDouble(String tag) throws Exception {
		try {
			String val = get( tag);
			return S.isNotNull( val) ? Double.valueOf( val) : 0;
		}
		catch( Exception e) {
			throw new Ex( "Tag %s is not a number", tag);
		}
	}
	
	public double getRequiredDouble(String tag) throws Exception {
		getRequiredString(tag);
		return getDouble(tag);
	}

	public int getRequiredInt(String tag) throws Exception {
		// keep this outside the try block so it throws a better exception
		String str = getRequiredString(tag);

		try {
			return Integer.valueOf(str); 
		}
		catch( Exception e) {
			throw new Ex( "Tag %s is not an integer", tag);
		}
	}

	/** Return int val or zero if null */
	public int getInt(String tag) throws Exception {
		try {
			String val = get(tag);
			return S.isNotNull(val) ? Integer.valueOf(val) : 0; 
		}
		catch( Exception e) {
			throw new Ex( "Tag %s is not an integer", tag);
		}
	}
	
	/** Put to the map but not the spreadsheet */
	public String putLocal(String tag, String newVal) {
		return super.put( tag, newVal);
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
					if (tag.equals( row.getString( m_col1) ) ) {
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
	
	public JsonObject toJson() {
		return new JsonObject( this);
	}
}
