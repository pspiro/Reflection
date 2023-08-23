package monitor;

import java.util.HashMap;

import org.json.simple.JsonArray;

import tw.util.MyTableModel;

class JsonModel extends MyTableModel {
	final String[] names;
	final HashMap<Integer,String> m = new HashMap<>(); // map index to name
	JsonArray m_ar = new JsonArray();
	
	JsonModel(String allNames) {
		names = allNames.split(",");
		
		for (int i = 0; i < names.length; i++) {
			m.put( i, names[i]);
		}
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
}