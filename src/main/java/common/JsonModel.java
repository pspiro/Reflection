package common;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellRenderer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.MyTableModel;
import tw.util.S;

public class JsonModel extends MyTableModel {
	final protected HashMap<Integer,String> m_namesMap = new HashMap<>(); // map index to name
	protected JsonArray m_ar = new JsonArray();  // can get replaced
	protected final String[] m_colNames;
	private String m_justify = "";
	int lastSortedCol = -1;

	public JsonModel(String allNames) {
		m_colNames = allNames.split(",");
		
		for (int i = 0; i < m_colNames.length; i++) {
			m_namesMap.put( i, m_colNames[i]);
		}
	}

	public JsonArray ar() {
		return m_ar;
	}
	
	/** Call firetabledatachanged() after this */
	public void setRows( JsonArray ar) {
		m_ar = ar;
	}
	
	/** Pass a string with one char for each column, either l or r */
	public void justify(String str) {
		m_justify = str;
	}
	
	protected int getColumnIndex(String name) {
		for (int i = 0; i < m_colNames.length; i++) {
			if (m_colNames[i].equals(name) ) {
				return i;
			}
		}
		return -1;
	}
	
	@Override public TableCellRenderer getRenderer(int row, int col) {
		return col < m_justify.length() && m_justify.charAt(col) == 'r' ? RIGHT_RENDERER : DEFAULT;
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
		Object val = m_ar.get(row).get(key);
		return val != null ? format( key, val) : null;
	}
	
	/** Sort on first column ascending */
	public void resetSort() {
		if (m_colNames.length > 0 && m_ar.isSortable(m_colNames[0])) {
			m_ar.sortJson( m_colNames[0], true); 
			lastSortedCol = 0;
		}
		else {
			lastSortedCol = -1;
		}
	}

	/** Sort when column is clicked */
	@Override public final void onHeaderClicked(int col) {
		if (col < m_colNames.length && m_ar.isSortable(m_colNames[col])) {
			m_ar.sortJson( m_colNames[col], col != lastSortedCol);
			lastSortedCol = col == lastSortedCol ? -1 : col;
			fireTableDataChanged();
		}
	}

	@Override final public void onDoubleClick(int row, int col) {
		String tag = m_namesMap.get(col);
		Object val = getValueAt(row, col);
		onDouble(tag, val);
	}

	@Override public final void onCtrlClick(MouseEvent e, int row, int col) {
		String tag = m_namesMap.get(col);
		Object val = getValueAt(row, col);
		onCtrlClick(m_ar.get(row), tag);
	}
	
	public JsonObject getRow(int i) {
		return m_ar.get(i);
	}

	public static JMenuItem menuItem(String text, ActionListener listener) {
		JMenuItem it = new JMenuItem(text);
		it.addActionListener(listener);
		return it;
	}
	
	@Override public final void onRightClick(MouseEvent e, int row, int col) {
		JPopupMenu m = new JPopupMenu();
		m.add( menuItem("Copy", ev -> copy(row, col) ) );
		m.add( menuItem("Delete", ev -> delete(row, col) ) );
		m.show( e.getComponent(), e.getX(), e.getY() );
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
	
	protected final String getTooltip(int row, int col) {
		return col < m_colNames.length && row < m_ar.size()
					? getTooltip( m_ar.get(row), m_colNames[col])
					: null;
	}
	
	/* ----- Override these methods ----- */
	
	/** Override this
	 * @param value is not null
	 * @return formatted value */
	protected Object format(String key, Object value) {
		return value;
	}
	
	/** Delete the row based on the first column which must be type string */ 
	protected void delete(int row, int col) {  // move this down
	}

	protected String getTooltip(JsonObject row, String tab) {
		return null;
	}
	
	protected void onDouble(String tag, Object val) {
	}

	protected void onCtrlClick(JsonObject row, String tag) {
	}

}
