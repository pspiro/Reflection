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
}
