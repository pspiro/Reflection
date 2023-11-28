package monitor;

import java.awt.BorderLayout;

import common.Util;
import http.MyClient;
import tw.util.S;

/** MktDataServer prices */
public class MdsPricesPanel extends JsonPanel {

	public MdsPricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,bid time,ask time,last time,from");
		add( m_model.createTable() );
		m_model.justify("llrrr");
	}
	
	@Override public void refresh() throws Exception {
		m_model.m_ar = MyClient.getArray(Monitor.m_config.mdBaseUrl() + "/mdserver/get-prices");
		m_model.fireTableDataChanged();
	}
	
	@Override protected Object format(String key, Object value) {
		return
				key.indexOf( "time") != -1 && value instanceof Long && ((Long)value) != 0
					? Util.yToS.format( value) :
				key.equals("bid") || key.equals("ask") || key.equals("last")
					? fmtPrice(value) 
					: value;
	}

	static Object fmtPrice(Object val) {
		try {
			return val != null ? S.fmt2d( Double.parseDouble(val.toString()) ) : val;
		}
		catch( Exception e) {
			return val;
		}
	}

}
