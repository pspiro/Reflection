package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.HtmlButton;
import tw.util.S;

/** Querys data from the database */
public class QueryPanel extends JsonPanel {
	final JTextField where = new JTextField(20);
	final LinkedList<String> m_list = new LinkedList<>();
	final String m_table;
	final String m_sql;
	
	QueryPanel(String table, String allNames, String sql) {
		super( new BorderLayout(), allNames);
		m_table = table;
		m_sql = sql;
		
		where.addActionListener( e -> {
			Util.wrap( () -> refresh() );
		});
		
		HtmlButton clr = new HtmlButton( "Clear", e -> {
			where.setText("");
			Util.wrap( () -> refresh() );
		});
		
		HtmlButton bak = new HtmlButton( "Back", e -> {
			if (m_list.size() >= 2) {
				m_list.pop();
				where.setText(m_list.pop());
				Util.wrap( () -> refresh() );
			}
		});
		
		JPanel topPanel = new JPanel();
		topPanel.add( bak);
		topPanel.add(Box.createHorizontalStrut(5));
		topPanel.add( new JLabel("Where: "));
		topPanel.add( where);
		topPanel.add(Box.createHorizontalStrut(5));
		topPanel.add( clr);
		
		where.addActionListener( e -> Monitor.refresh() );
		
		add( topPanel, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	protected JsonModel createModel( String allNames) {
		return new QueryModel(allNames);
	}

	@Override protected Object format(String key, Object value) {
		if (key.equals("created_at") ) {
			return Util.left( value.toString(), 22);
		}
		return super.format(key, value);
	}
	

	@Override protected void onCtrlClick(JsonObject row, String tag) {
		Util.wrap( () -> {
			String val = Util.ask( "Enter new value for %s field", tag);
			
			Monitor.m_config.sqlCommand( sql -> sql.updateJson( 
					m_table, 
					Util.toJson( tag, val), 
					"wallet_public_key = '%s'",  // this should be passed in constructor. pas 
					row.getString("wallet_public_key") ) );
			refresh();
		});
	}

	@Override protected void onDouble(String tag, Object val) {
		where.setText( String.format( "where %s = '%s'", tag, val) );
		Util.wrap( () -> refresh() );
	}
	
	@Override protected void refresh() throws Exception {
		S.out( "Refreshing QueryModel");
		m_list.push(where.getText());
		
		String str = m_sql
				.replaceAll( "\\$limit", "limit " + Monitor.num() )
				.replaceAll( "\\$where", where.getText() );
		
		setRows( Monitor.m_config.sqlQuery( conn -> conn.queryToJson(str) ) );
		rows().forEach( obj -> adjust(obj) );  // or override format() to keep the object intact
		
		// sort by first column but it will retain forward/reverse ordering
		m_model.onHeaderClicked(0);

		m_model.fireTableDataChanged();
		S.out( "Refreshed query model to %s", rows().size() );
	}
	
	/** This panel is for SQL queries to the database */
	class QueryModel extends JsonPanelModel {
		QueryModel( String allNames) {
			super( allNames);
		}
		
		/** Delete the row based on the first column which must be type string */ 
		@Override protected void delete(int row, int col) {
			try {
				Monitor.m_config.sqlCommand( sql -> 
					sql.delete( "delete from %s where %s = '%s'", m_table, m_namesMap.get(0), getValueAt(row, col) ) );
				fireTableDataChanged();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}

	public void adjust(JsonObject obj) {
	}
	
	/** Override me */
	public void onRightClick(MouseEvent e, int row, int col) {
	}
	
	public void clear() {
		rows().clear();
		m_model.fireTableDataChanged();
	}

}
