package monitor;

import java.awt.LayoutManager;
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

import common.Util;
import monitor.Monitor.MonPanel;
import tw.util.MyTableModel;
import tw.util.NewTabbedPanel.INewTab;
import tw.util.S;

/** Panel with a table that contains rows of Json objects; each column header is a key 
 *  in the Json table */
public abstract class JsonPanel extends MonPanel implements INewTab {
	final JsonModel m_model;
	
	public JsonPanel(LayoutManager layout, String allNames) {
		super(layout);
		m_model = createModel(allNames);
	}
	
	JsonModel createModel(String allNames) {
		return new JsonModel(allNames);
	}

	/** Show only rows that have the clicked-on value */
	void onDouble(String tag, Object allowed) {
	}

	/** Override this
	 * @param value could be null
	 * @return formatted value */
	protected Object format(String key, Object value) {
		return value;
	}
	
	protected String getTooltip(int row, String tag) {
		return null;
	}
	
	@Override public void activated() {
		S.out( "Activating JsonPanel");
		Util.wrap( () -> refresh() );
	}

	public void refresh() throws Exception {
		S.out( "Refreshing JsonPanel");
		m_model.refresh();
	}

	public class JsonModel extends MyTableModel {
		final HashMap<Integer,String> m_namesMap = new HashMap<>(); // map index to name
		JsonArray m_ar = new JsonArray();  // can get replaced
		protected final String[] m_colNames;
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
		
		public void onHeaderClicked(int col) {
			if (col < m_colNames.length && m_ar.isSortable(m_colNames[col])) {
				m_ar.sortJson( m_colNames[col] );
				fireTableDataChanged();
			}
		}
		
		@Override final public void onDoubleClick(int row, int col) {
			String tag = m_namesMap.get(col);
			Object val = getValueAt(row, col);
			onDouble(tag, val);
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
		
		/** Delete the row based on the first column which must be type string */ 
		void delete(int row, int col) {
		}
		
		protected String getTooltip(int row, int col) {
			return col < m_colNames.length && row < m_ar.size()
						? JsonPanel.this.getTooltip(row, m_colNames[col])
						: null;
		}
	}

}
