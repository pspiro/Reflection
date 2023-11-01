package monitor;

import java.awt.BorderLayout;

import common.Util;
import http.MyClient;

/** MktDataServer prices */
public class MdsPricesPanel extends JsonPanel {

	public MdsPricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,bid time,ask time,last time,from");
		add( m_model.createTable() );
	}
	
	@Override public void refresh() throws Exception {
		m_model.m_ar = MyClient.getArray(Monitor.mdsBase + "/mdserver/getPrices");
		m_model.fireTableDataChanged();
	}
	
	@Override protected Object format(String key, Object value) {
		return key.indexOf( "time") != -1 && value instanceof Long && ((Long)value) != 0
				? Util.yToS.format( value)
				: value;
	}

}
