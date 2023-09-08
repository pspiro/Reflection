package monitor;

import java.util.HashMap;

import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.MyTableModel;

class JsonModel extends MyTableModel {
	final String[] names;
	final HashMap<Integer,String> m = new HashMap<>(); // map index to name
	JsonArray m_ar = new JsonArray();
	private String m_justify = "";
	
	JsonModel(String allNames) {
		names = allNames.split(",");
		
		for (int i = 0; i < names.length; i++) {
			m.put( i, names[i]);
		}
	}
	
	/** Pass a string with one char for each column, either l or r */
	void justify(String str) {
		m_justify = str;
	}
	
	@Override public TableCellRenderer getRenderer(int row, int col) {
		return col < m_justify.length() && m_justify.charAt(col) == 'r' ? RIGHT_RENDERER : DEFAULT;
	}
	
	int getColumnIndex(String name) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(name) ) {
				return i;
			}
		}
		return -1;
	}
	
	void refresh( ) throws Exception { // needed?
	}
	
	@Override public int getRowCount() {
		return m_ar.size();
	}

	@Override public int getColumnCount() {
		return m.size();
	}
	
	@Override public String getColumnName(int col) {
		return names[col];
	}

	@Override public Object getValueAt(int row, int col) {
		return m_ar.get(row).get( m.get(col) );
	}
	
	public void onHeaderClicked(int col) {
		m_ar.sortJson( names[col] );
		fireTableDataChanged();
	}
	
	JsonObject getRow(int i) {
		return m_ar.get(i);
	}
	
}