package monitor;

import java.awt.BorderLayout;

import http.MyClient;
import tw.util.MyTable;


public class LiveOrdersPanel extends JsonPanel {
	static final String allNames = "uid,wallet,action,description,progress,status,errorCode,errorText";
	static final String endpoint = "/api/all-live-orders";

	LiveOrdersPanel() {
		super( new BorderLayout(), allNames);
		
		add( new MyTable(m_model).scroll() );
	}
	
	public void refresh() throws Exception {
		MyClient.getArray(Monitor.refApiBaseUrl() + endpoint, ar -> {
			m_model.m_ar = ar;
			m_model.fireTableDataChanged();
		});
	}
}
