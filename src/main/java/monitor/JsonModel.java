package monitor;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.MyTableModel;
import tw.util.S;

abstract class JsonModel extends MyTableModel {
	final String[] m_colNames;
	final HashMap<Integer,String> m_namesMap = new HashMap<>(); // map index to name
	JsonArray m_ar = new JsonArray();
	private String m_justify = "";
	
	JsonModel(String allNames) {
		m_colNames = allNames.split(",");
		
		for (int i = 0; i < m_colNames.length; i++) {
			m_namesMap.put( i, m_colNames[i]);
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
		for (int i = 0; i < m_colNames.length; i++) {
			if (m_colNames[i].equals(name) ) {
				return i;
			}
		}
		return -1;
	}
	
	abstract void refresh() throws Exception;
	
	@Override public int getRowCount() {
		return m_ar.size();
	}

	@Override public int getColumnCount() {
		return m_namesMap.size();
	}
	
	@Override public String getColumnName(int col) {
		return m_colNames[col];
	}

	@Override public Object getValueAt(int row, int col) {
		return format( m_ar.get(row).get( m_namesMap.get(col) ) );
	}
	
	protected Object format(Object value) { // you would have to add tag or col to be useful
		return value;
	}

	public void onHeaderClicked(int col) {
		m_ar.sortJson( m_colNames[col] );
		fireTableDataChanged();
	}
	
	@Override public void onDoubleClick(int row, int col) {
		String tag = m_namesMap.get(col);
		Object allowed = getValueAt(row, col);
		
		for (Iterator<JsonObject> iter = m_ar.iterator(); iter.hasNext(); ) {
			Object val = iter.next().get(tag);
			if (val == null || !val.equals(allowed) ) {
				iter.remove();
			}
		}
		
		fireTableDataChanged();
	}
	
	JsonObject getRow(int i) {
		return m_ar.get(i);
	}
	
	@Override public void onRightClick(MouseEvent e, int row, int col) {
		Object obj = getValueAt(row, col);
		
		if (obj != null) {
	        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
	        StringSelection strse1 = new StringSelection(obj.toString());
	        clip.setContents(strse1, strse1);
	        S.out( "Copyied %s to cliboard", obj);
		}
	}
	
}