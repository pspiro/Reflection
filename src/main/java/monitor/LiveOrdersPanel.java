package monitor;

import java.awt.BorderLayout;

import tw.util.MyTable;
import tw.util.S;


public class LiveOrdersPanel extends JsonPanel {
	static final String endpoint = "/api/all-live-orders";

	final JsonModel m_model;
	
	LiveOrdersPanel() {
		super( new BorderLayout() );
		
		m_model = new Model();
		
		add( new MyTable(m_model).scroll() );
	}
	
	protected JsonModel createModel(String allNames, String sql) {
		return new Model();
	}

	public void refresh() throws Exception {
		S.out( "Refreshing Live Orders panel");
		m_model.refresh();
	}
	
	class Model extends JsonModel {
		Model() {
			super( "uid,wallet,action,description,progress,status,errorCode,errorText");
		}
		
		void refresh( ) throws Exception {
			super.refresh();
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
