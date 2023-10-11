package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.HtmlButton;
import tw.util.S;

/** Querys data from the database */
public class QueryPanel extends JsonPanel {
	final JsonModel m_model;
	final JTextField where = new JTextField(20);
	final LinkedList<String> m_list = new LinkedList<>();
	
	QueryPanel(String table, String allNames, String sql) {
		super( new BorderLayout() );
		
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
		
		where.addActionListener( e -> Monitor.m.refresh() );
		
		m_model = createModel(table, allNames, sql);
		
		add( topPanel, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	protected JsonModel createModel(String table, String allNames, String sql) {
		return new QueryModel(table, allNames, sql);
	}

	public void refresh() throws Exception {
		m_model.refresh();
	}
	
	class QueryModel extends JsonModel {
		final String m_table;
		final String m_sql;

		QueryModel( String table, String allNames, String sql) {
			super( allNames);
			m_table = table;
			m_sql = sql;
		}
		
		@Override void onDouble(String tag, Object val) {
			where.setText( String.format( "where %s = '%s'", tag, val) );
			Util.wrap( () -> refresh() );
		}
		
		@Override void refresh( ) throws Exception {
			m_list.push(where.getText());
			
			String str = m_sql
					.replaceAll( "\\$limit", "limit " + Monitor.num() )
					.replaceAll( "\\$where", where.getText() );
			
			m_ar = Monitor.m_config.sqlQuery( conn -> conn.queryToJson(str) );
			m_ar.forEach( obj -> adjust(obj) );  // or override format() to keep the object intact
			onHeaderClicked(0);
			S.out( "Refreshed query model to %s", m_ar.size() ); 
		}

		public void adjust(JsonObject obj) {
		}

		/** Delete the row based on the first column which must be type string */ 
		@Override void delete(int row, int col) {
			try {
				Monitor.m_config.sqlCommand( sql -> 
					sql.delete( "delete from %s where %s = '%s'", m_table, m_namesMap.get(0), getValueAt(row, col) ) );
				fireTableDataChanged();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}

	@Override public void activated() {
		Util.wrap( () -> refresh() );
	}

	public void adjust(JsonObject obj) {
	}
	
	/** Override me */
	public void onRightClick(MouseEvent e, int row, int col) {
	}

	@Override public void closed() {
	}

}
