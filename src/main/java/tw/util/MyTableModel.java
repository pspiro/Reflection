package tw.util;

import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public abstract class MyTableModel extends AbstractTableModel {
	public static DefaultTableCellRenderer DEFAULT = new DefaultTableCellRenderer();
	public static RightRenderer RIGHT_RENDERER = new RightRenderer(); // right-justify
	
	public static class RightRenderer extends DefaultTableCellRenderer {
		public RightRenderer() {
			setHorizontalAlignment(JLabel.RIGHT);
		}
	}

	public TableCellRenderer getRenderer(int row, int col) {
		return MyTableModel.DEFAULT;
	}

	public void onLeftClick(MouseEvent e, int row, int col) {
	}

	public void onRightClick(MouseEvent e, int row, int col) {
	}

	public void onCtrlClick(MouseEvent e, int row, int col) {
	}

	public void onDoubleClick(int row, int col) {
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void onHeaderClicked(int col) {
	}

	public void selectionChanged(ListSelectionEvent e) {
	}

	public JScrollPane createTable( String title) {
		var panel = createTable();
		panel.setBorder( new TitledBorder( title) );
		return panel;
	}
	
	public JScrollPane createTable() {
		return new JScrollPane( new MyTable( this) );
	}

	public MyTable createNoScroll() {
		return new MyTable( this);
	}

	protected String getTooltip(int row, int col) {
		return null;
	}
	
	/** you can set the easily set the column names and lef/right justification
	 *  by setting two strings in the subclass constructor */
	public static abstract class SimpleTableModel extends MyTableModel {
		// set these two in the subclass constructor
		protected String[] columnNames;
		protected String justification;  // string of 'l' (letter el) and 'r'
		
		// better would be a single string or array of alternating names and justification. pas

		@Override public final int getColumnCount() {
			return columnNames.length;
		}
		@Override public final String getColumnName(int col) {
			return col < columnNames.length ? columnNames[col] : "";
		}
		@Override public TableCellRenderer getRenderer(int row, int col) {
			return col < justification.length() && justification.charAt( col) == 'r' ? RIGHT_RENDERER : DEFAULT;
		}

		/** catch exceptions so subclass doesn't have to, and don't display stack
		 *  trace for index out of bounds which can happen when rows are deleted */
		@Override public final Object getValueAt(int rowIndex, int columnIndex) {
			try {
				return getValueAt_( rowIndex, columnIndex);
			}
			catch( IndexOutOfBoundsException e) {
				S.out( "Error - " + e);
			}
			catch( Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public abstract Object getValueAt_(int row, int col) throws Exception;
	}

}
