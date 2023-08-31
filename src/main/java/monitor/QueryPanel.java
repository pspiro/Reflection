package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.json.simple.JsonObject;

import monitor.Monitor.RefPanel;
import tw.util.S;

public class QueryPanel extends JPanel implements RefPanel {
	final JsonModel m_model;
	
	QueryPanel(String allNames, String sql) {
		super( new BorderLayout() );
		
		m_model = createModel(allNames, sql);
		
		add( m_model.createTable() );
	}
	
	protected JsonModel createModel(String allNames, String sql) {
		return new QueryModel(allNames, sql);
	}

	public void refresh() throws Exception {
		S.out( "Refreshing Trans panel");
		m_model.refresh();
	}
	
	static class QueryModel extends JsonModel {
		final String m_sql;

		QueryModel( String allNames, String sql) {
			super( allNames);
			m_sql = sql;
		}
		
		void refresh( ) throws Exception {
			m_ar = Monitor.m_config.sqlQuery( conn -> conn.queryToJson(m_sql) );
			m_ar.forEach( obj -> adjust(obj) );
			fireTableDataChanged();
		}

		public void adjust(JsonObject obj) {
		}
	}

	@Override public void activated() {
		try {
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override public void closed() {
	}
}
