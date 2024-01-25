package monitor;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JPanel;

import common.Util;
import http.MyClient;
import tw.util.HtmlButton;
import tw.util.S;

/** MktDataServer prices */
public class MdsPricesPanel extends JsonPanel {

	public MdsPricesPanel() {
		super( new BorderLayout(), "symbol,conid,bidSize,bid,ask,askSize,last,bid time,ask time,last time,from");
		add( m_model.createTable() );
		m_model.justify("llrrr");
		
		JPanel butPanel = new JPanel();
		butPanel.add( new HtmlButton("Disconnect/Reconnect", act -> reconnect() ) );
		butPanel.add( Box.createHorizontalStrut(20) );
		butPanel.add( new HtmlButton("Refresh Symbols", act -> refreshMdServer() ) );
		add( butPanel, BorderLayout.NORTH);
	}
	
	void refreshMdServer() {
		Util.wrap( () -> {
			// tell MdServer to re-read symbols list and resubscribe market data
			String resp = MyClient.getString(Monitor.m_config.mdBaseUrl() + "/mdserver/refresh");
			refresh();
			Util.inform(this, resp);
		});
	}
	
	private void reconnect() {
		Util.wrap( () -> {
			// tell MdServer to disconnect/reconnect
			String resp = MyClient.getString(Monitor.m_config.mdBaseUrl() + "/mdserver/disconnect");
			Util.inform(this, resp);
		});
	}

	@Override public void refresh() throws Exception {
		S.out( "Refreshing mdserver prices");
		setRows( MyClient.getArray(Monitor.m_config.mdBaseUrl() + "/mdserver/get-prices") );
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
