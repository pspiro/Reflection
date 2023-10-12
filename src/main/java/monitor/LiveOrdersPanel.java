package monitor;

import java.awt.BorderLayout;

import http.MyClient;
import tw.util.MyTable;
import tw.util.S;


public class LiveOrdersPanel extends JsonPanel {
	static final String allNames = "uid,wallet,action,description,progress,status,errorCode,errorText";
	static final String endpoint = "/api/all-live-orders";

	LiveOrdersPanel() {
		super( new BorderLayout(), allNames);
		
		add( new MyTable(m_model).scroll() );
	}
	
	protected JsonModel createModel(String allNames) {
		return new Model(allNames);
	}

	public void refresh() throws Exception {
		S.out( "Refreshing Live Orders panel");
		m_model.refresh();
	}
	
	class Model extends JsonModel {
		Model(String allNames) {
			super(allNames);
		}
		
		void refresh( ) throws Exception {
			super.refresh();
			MyClient.getArray(Monitor.base + endpoint, ar -> {
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
