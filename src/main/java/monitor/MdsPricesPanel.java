package monitor;

import java.awt.BorderLayout;

import common.Util;
import http.MyClient;
import tw.util.S;

/** MktDataServer prices */
public class MdsPricesPanel extends JsonPanel {

	public MdsPricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,bid time,ask time,last time,bidSize,askSize,from");
		add( m_model.createTable() );
		m_model.justify("llrrr");
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing mdserver prices");
		m_model.m_ar = MyClient.getArray(Monitor.m_config.mdBaseUrl() + "/mdserver/get-prices");
		m_model.fireTableDataChanged();
	}
	
	@Override protected Object format(String key, Object value) {
		return
				key.indexOf( "time") != -1 && value instanceof Long && ((Long)value) != 0
					? Util.yToS.format( value) :
				key.equals("bid") || key.equals("ask") || key.equals("last")
					? fmtPrice(value) :
				key.equals("bidSize") || key.equals("askSize")
					? fmtSize(value)
					: value;
	}

	private Object fmtSize(Object val) {
		return val instanceof Double ? (int)(double)val : val; 
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
