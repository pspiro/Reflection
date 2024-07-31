package monitor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.HtmlButton;
import tw.util.MyTable;
import tw.util.S;


public class LiveOrdersPanel extends JsonPanel {
	static final String allNames = "createdAt,uid,wallet,action,description,progress,status,errorCode,errorText";
	static final String endpoint = "/api/all-live-orders";

	LiveOrdersPanel() {
		super( new BorderLayout(), allNames);

		add( new HtmlButton( "Clear All", this::clearAll), BorderLayout.NORTH);
		add( new MyTable(m_model).scroll() );
	}
	
	void clearAll(ActionEvent e) {
		try {
			JsonObject json = MyClient.getJson(Monitor.refApiBaseUrl() + "/api/clear-live-orders");
			json.display();
			refresh();
			Util.inform( this, "%s %s", json.getString( "code"), json.getString( "message") );
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@Override protected Object format(String key, Object value) {
		return switch (key) {
		case "createdAt" -> value != null && S.isNotNull( value.toString() ) ? Util.yToS.format( value) : value; 
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
