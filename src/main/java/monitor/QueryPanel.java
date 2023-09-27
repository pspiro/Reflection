package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.Util;
import monitor.Monitor.RefPanel;
import tw.util.HtmlButton;
import tw.util.S;

/** Querys data from the database */
public class QueryPanel extends JPanel implements RefPanel {
	final JsonModel m_model;
	final JTextField where = new JTextField(20);
	
	QueryPanel(String allNames, String sql) {
		super( new BorderLayout() );
		
		HtmlButton clr = new HtmlButton( "Clear", e -> {
			where.setText("");
			Util.wrap( () -> refresh() );
		});
		
		JPanel topPanel = new JPanel();
		topPanel.add( new JLabel("Where: "));
		topPanel.add( where);
		topPanel.add( clr);
		
		where.addActionListener( e -> Monitor.m.refresh() );
		
		m_model = createModel(allNames, sql);
		
		add( topPanel, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	protected JsonModel createModel(String allNames, String sql) {
		return new QueryModel(allNames, sql);
	}

	public void refresh() throws Exception {
		m_model.refresh();
	}
	
	class QueryModel extends JsonModel {
		final String m_sql;

		QueryModel( String allNames, String sql) {
			super( allNames);
			m_sql = sql;
		}
		
		@Override void onDouble(String tag, Object val) {
			where.setText( String.format( "where %s = '%s'", tag, val) );
			Util.wrap( () -> refresh() );
		}
		
		@Override void refresh( ) throws Exception {
			String str = m_sql
					.replaceAll( "\\$limit", "limit " + Monitor.num() )
					.replaceAll( "\\$where", where.getText() );
			
			m_ar = Monitor.m_config.sqlQuery( conn -> conn.queryToJson(str) );
			m_ar.forEach( obj -> adjust(obj) );  // or override format() to keep the object intact 
			fireTableDataChanged();
			S.out( "Refreshed query model to %s", m_ar.size() ); 
		}

		public void adjust(JsonObject obj) {
		}
	}

	@Override public void activated() {
		Util.wrap( () -> refresh() );
	}

	/** Override me */
	public void onRightClick(MouseEvent e, int row, int col) {
	}

	@Override public void closed() {
	}
}
