package monitor;

import java.awt.BorderLayout;

import org.json.simple.JsonArray;

import http.MyClient;
import reflection.Stock;

/** This tracks the UserTokenMgr which subtracts out the quantity of live order 
 * so as not to double-spend crypto */
public class UserTokenPanel extends JsonPanel {

	public UserTokenPanel() {
		super(new BorderLayout(), "wallet,token,offset");
		add( m_model.createTable() );
	}

	@Override protected void refresh() throws Exception {
		 JsonArray ar = MyClient.getArray(Monitor.m_config.baseUrl() + "/api/user-token-mgr");
		 ar.update( "token", token -> updated( (String)token) );
		 m_model.setRows( ar);
		 m_model.fireTableDataChanged();
	}

	private String updated(String tokenAddr) {
		String ret = tokenAddr;
		try {
			Stock stock = Monitor.stocks.getStockByTokenAddr(tokenAddr);
			if (stock != null) {
				ret = stock.getString("symbol");
			}
			else if (tokenAddr.equalsIgnoreCase( Monitor.m_config.rusdAddr() ) ) {
				ret = Monitor.m_config.rusd().name();
			}
			else if (tokenAddr.equalsIgnoreCase( Monitor.m_config.busd().address() ) ) {
				ret = Monitor.m_config.busd().name();
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}
