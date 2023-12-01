package tw.util;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class MyTable extends JTable { // nicole
	@Override public MyTableModel getModel() { return (MyTableModel)super.getModel(); }

	public MyTable(MyTableModel m) { // inbox
		super( m);
		
		getTableHeader().setReorderingAllowed(false);		
		
		// list for events on the column headers
		getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int col = columnAtPoint( e.getPoint() );
				getModel().onHeaderClicked( col);
			}
		});

		// listen for events in the table
		addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) ) {
					getModel().onRightClick(e, rowAtPoint(e.getPoint()), columnAtPoint(e.getPoint()) );
				}
				else if (e.getClickCount() == 2) {
					int row = rowAtPoint( e.getPoint() );
					int col = MyTable.this.getTableHeader().columnAtPoint( e.getPoint() );
					getModel().onDoubleClick(row, col);
				}
				else if (SwingUtilities.isLeftMouseButton(e)) {
					getModel().onLeftClick(e, rowAtPoint(e.getPoint()), columnAtPoint(e.getPoint()) );
				}
			}
		});
		
	    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				selectionChanged(e);
			}
		} );
	 		
	}
	
	public void setSingleClick() {
		DefaultCellEditor singleclick = new DefaultCellEditor(new JTextField());
	    singleclick.setClickCountToStart(1);
	    
	    for (int i = 0; i < getColumnCount(); i++) {
	        setDefaultEditor(getColumnClass(i), singleclick);
	    } 
	}
	
	public void setColumnWidth( int i, int width) {
		getColumnModel().getColumn(i).setPreferredWidth(width);
	}
	
	public void setPadding( int width) {
		getColumnModel().setColumnMargin( width);
	}
	
	protected void selectionChanged(ListSelectionEvent e) {
	}

	@Override public TableCellRenderer getCellRenderer(int row, int col) {
		return getModel().getRenderer( row, col);
	}
	
	@Override public boolean isCellEditable(int row, int col) {
		return getModel().isCellEditable( row, col);
	}
	
	@Override public String getToolTipText(MouseEvent e) {
		return getModel().getTooltip(
				rowAtPoint( e.getPoint() ),
				columnAtPoint( e.getPoint() ) );
	}

	static class MyDefaultCellEditor extends DefaultCellEditor {
		public MyDefaultCellEditor(JTextField textField) {
			super(textField);
		}
		
		@Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,	int column) {
			JTextField f = (JTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
//			//f.setPreferredSize( new Dimension( 5, 5) );
			return f;
		}		
	}
	
	
//	DefaultCellEditor ed = new DefaultCellEditor(new MyTextField()); // this doesn't work because we need to sublcass GenericEditor which is so fucking stupid because you can't
//	
//	@Override public TableCellEditor getCellEditor(int row, int column) {
//		return ed;
//	}
	
	

	/** Resize last column to take up remaining space. */
	public void resizeLastColumn( int totalWidth) {
		int width = 0;
		TableColumnModel mod = getColumnModel();
		for (int i = 0; i < mod.getColumnCount(); i++) {
			TableColumn col = mod.getColumn( i);

			if (i < mod.getColumnCount() - 1) {
				width += col.getWidth();
			}
			else {
				col.setPreferredWidth( totalWidth - width);
			}
		}
		revalidate();
		repaint();
	}
	
	public JScrollPane scroll() {
		return new JScrollPane(this);
	}
	
//	@Override public boolean editCellAt(int row, int column, EventObject e) {
//		return getModel().editCellAtsuper.editCellAt(row, column, e);
//	}
	
}
