package monitor;

import java.awt.BorderLayout;

import common.Util;
import http.MyClient;
import tw.util.MyTable;


public class LiveOrdersPanel extends JsonPanel {
	static final String allNames = "createdAt,uid,wallet,action,description,progress,status,errorCode,errorText";
	static final String endpoint = "/api/all-live-orders";

	LiveOrdersPanel() {
		super( new BorderLayout(), allNames);
		
		add( new MyTable(m_model).scroll() );
	}
	
	@Override protected Object format(String key, Object value) {
		return switch (key) {
		case "ceatedAt" -> value instanceof Long ? Util.yToS.format( (long)value) : value; 
		default -> value;
		};
	}
	
	public void refresh() throws Exception {
		MyClient.getArray(Monitor.refApiBaseUrl() + endpoint, ar -> {
			setRows( ar);
			m_model.fireTableDataChanged();
		});
	}
}
