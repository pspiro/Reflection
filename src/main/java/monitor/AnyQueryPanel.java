package monitor;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.json.simple.JsonArray;

import common.Util;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

@SuppressWarnings("rawtypes")
class AnyQueryPanel extends JsonPanel {
	private MyComboBox m_sel = new MyComboBox();
	private GTable tab;
	
	AnyQueryPanel() throws Exception {
		super( new BorderLayout(), "");
		
		JPanel north = new JPanel();
		north.add( m_sel);

		add( north, BorderLayout.NORTH);
		add( m_model.createTable() );  // don't move this, WalletPanel adds to a different place

		m_sel.addActionListener( event -> Util.wrap( () -> run() ) );
	}
	
	@SuppressWarnings("unchecked")
	@Override protected void refresh() throws Exception {
		tab = new GTable( NewSheet.Reflection, "Queries", "Name", "Query");
		m_sel.setModel( new DefaultComboBoxModel( tab.keySet().toArray() ) );
		
		setRows( new JsonArray() );
		m_model.fireTableDataChanged();
		
		m_sel.setSelectedItem( null);
	}
	
	void run() throws Exception {
		Util.iff( m_sel.getString(), queryName -> {
			String query = tab.get( queryName);
			
			// run query
			JsonArray rows = Monitor.m_config.sqlQuery( query);

			// update column names
			String[] names = rows.getKeys().toArray( new String[0]);
			String str = String.join( ",", names);
			m_model.setNames( str);
			m_model.fireTableStructureChanged();

			setRows( rows);
			
			m_model.resetSort();  // sort by first column if it is sortable
			m_model.fireTableDataChanged();
			
			S.out( "***Refreshed query model to %s", rows().size() );
		});
	}

	static class MyComboBox extends JComboBox<Object> {
		MyComboBox(Object[] data) {
			this();
			set( data);
		}

		MyComboBox() {
			super( "wwwwwwwwwwwww".split(",") ); 
		}
		
		Object getSelected() {
			Object[] objs = getSelectedObjects();
			return objs != null && objs.length > 0 ? objs[0] : null;
		}
		
		String getString() {
			return getSelected() != null ? getSelected().toString() : null;
		}
		
		void set( Vector data) {
			setModel( new DefaultComboBoxModel( data) );			
		}
		
		void set( Object[] data) {
			setModel( new DefaultComboBoxModel( data) );			
		}
	}
	
}
