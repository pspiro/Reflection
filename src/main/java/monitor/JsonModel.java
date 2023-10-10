package monitor;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.MyTableModel;
import tw.util.S;

public class JsonModel extends MyTableModel {
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
	
	void refresh() throws Exception {
		m_filtered = false;
	}
	
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
		String key = m_namesMap.get(col);
		return format( key, m_ar.get(row).get(key) );
	}
	
	protected Object format(String key, Object value) { // you would have to add tag or col to be useful
		return value;
	}

	public void onHeaderClicked(int col) {
		if (col < m_colNames.length) {
			m_ar.sortJson( m_colNames[col] );
			fireTableDataChanged();
		}
	}
	
	boolean m_filtered;
	
	@Override public void onDoubleClick(int row, int col) {
		String tag = m_namesMap.get(col);
		Object val = getValueAt(row, col);
		onDouble(tag, val);
	}
	
	void onDouble(String tag, Object allowed) {
		if (m_filtered) {
			try {
				refresh();
			} catch (Exception e) {
				e.printStackTrace();
				return;  // we have no hope
			}
		}
		
		for (Iterator<JsonObject> iter = m_ar.iterator(); iter.hasNext(); ) {
			Object val = iter.next().get(tag);
			if (val == null || !val.equals(allowed) ) {
				iter.remove();
			}
		}
	
		fireTableDataChanged();
		m_filtered = true;
	}
	
	JsonObject getRow(int i) {
		return m_ar.get(i);
	}

	public static JMenuItem menuItem(String text, ActionListener listener) {
		JMenuItem it = new JMenuItem(text);
		it.addActionListener(listener);
		return it;
	}
	
	@Override public void onRightClick(MouseEvent e, int row, int col) {
		JPopupMenu m = new JPopupMenu();
		m.add( menuItem("Copy", ev -> copy(row, col) ) );
		m.add( menuItem("Delete", ev -> delete(row, col) ) );
		m.show( e.getComponent(), 0, 0);
	}
	
	private void copy(int row, int col) {
		Object obj = getValueAt(row, col);
		if (obj != null) {
	        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
	        StringSelection strse1 = new StringSelection(obj.toString());
	        clip.setContents(strse1, strse1);
	        S.out( "Copied %s to cliboard", obj);
		}
	}
	
	/** Delete the row based on the first column which must be type string */ 
	void delete(int row, int col) {
	}
}