package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

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
		
		JPanel butPanel = new JPanel( new FlowLayout( FlowLayout.CENTER, 15, 8) );
		butPanel.add( new HtmlButton("Disconnect/Reconnect", act -> send("disconnect") ) );;
		butPanel.add( new HtmlButton("Refresh Symbols", act -> send("refresh")) );
		butPanel.add( new HtmlButton("Debug on", act -> send("debug-on") ) );
		butPanel.add( new HtmlButton("Debug off", act -> send("debug-off") ) );
		add( butPanel, BorderLayout.NORTH);
	}
	

	private void send( String command) {
		wrap( () -> {
			String resp = MyClient.getString(Monitor.m_config.mdBaseUrl() + "/mdserver/" + command);
			refresh();
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
