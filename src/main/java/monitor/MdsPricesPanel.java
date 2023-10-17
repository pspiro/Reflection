package monitor;

import java.awt.BorderLayout;

import http.MyClient;

public class MdsPricesPanel extends JsonPanel {

	public MdsPricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,bid time,ask time,last time,from");
		add( m_model.createTable() );
	}
	
	@Override public void refresh() throws Exception {
		m_model.m_ar = MyClient.getArray(Monitor.base + "/mdserver/getPrices");
		m_model.fireTableDataChanged();
	}

}
