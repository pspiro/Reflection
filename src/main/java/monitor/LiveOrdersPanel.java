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

	class Model extends JsonModel {
		Model(String allNames) {
			super(allNames);
		}
		
		void refresh( ) throws Exception {
			super.refresh();
			MyClient.getArray(Monitor.refApiBaseUrl() + endpoint, ar -> {
				m_ar = ar;
				fireTableDataChanged();
			});
		}
	}
}
