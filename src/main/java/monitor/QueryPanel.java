package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import monitor.Monitor.RefPanel;
import tw.util.MyTable;
import tw.util.S;

public class QueryPanel extends JPanel implements RefPanel {
	final JsonModel m_mod;
	
	QueryPanel(String allNames, String sql) {
		super( new BorderLayout() );
		m_mod = new QueryModel(allNames, sql);
		
		add( new JScrollPane( new MyTable(m_mod) ) );
	}
	
	public void refresh() throws Exception {
		S.out( "Refreshing Trans panel");
		m_mod.refresh();
	}
	
	class QueryModel extends JsonModel {
		final String m_sql;

		QueryModel( String allNames, String sql) {
			super( allNames);
			m_sql = sql;
		}
		
		void refresh( ) throws Exception {
			m_ar = Monitor.m_config.sqlQuery( conn -> conn.queryToJson(m_sql) );
			fireTableDataChanged();
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
