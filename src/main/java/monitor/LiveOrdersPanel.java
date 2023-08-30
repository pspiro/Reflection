package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import common.Util;
import monitor.Monitor.RefPanel;
import tw.util.MyTable;
import tw.util.S;


public class LiveOrdersPanel extends JPanel implements RefPanel {
	String endpoint = "/api/all-live-orders";

	final JsonModel m_mod;
	
	LiveOrdersPanel() {
		super( new BorderLayout() );
		
		m_mod = new Model();
		
		add( new MyTable(m_mod).scroll() );
	}
	
	protected JsonModel createModel(String allNames, String sql) {
		return new Model();
	}

	public void refresh() throws Exception {
		S.out( "Refreshing Live Orders panel");
		m_mod.refresh();
	}
	
	class Model extends JsonModel {
		Model() {
			super( "id,action,description,progress");
		}
		
		void refresh( ) throws Exception {
			Monitor.queryArray(endpoint, ar -> {
				m_ar = ar;
				fireTableDataChanged();
			});
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
