package monitor;

import java.awt.BorderLayout;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.HtmlButton;

/** This tracks the UserTokenMgr which subtracts out the quantity of live order 
 * so as not to double-spend crypto */
public class UserTokenPanel extends JsonPanel {

	public UserTokenPanel() {
		super(new BorderLayout(), "createdAt,updatedAt,wallet,token,offset");
		add( new HtmlButton( "Reset", ev -> reset() ), BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	private void reset() {
		wrap( () -> {
		String url = String.format( "%s/api/reset-user-token-mgr", Monitor.refApiBaseUrl() );
		JsonObject json = MyClient.getJson( url);
		json.display();
		Util.inform( this, json.getString( "code"));
		});
	}

	@Override protected Object format(String key, Object value) {
		return switch (key) {
			case "createdAt" -> Util.yToS.format( (long)value);
			case "updatedAt" -> Util.yToS.format( (long)value);
			default -> value;
		};
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
			var token = Monitor.chain().getTokenByAddress(tokenAddr);
			if (token != null) {
				ret = token.name();
			}
			else if (tokenAddr.equalsIgnoreCase( Monitor.chain().rusd().address() ) ) {
				ret = Monitor.m_config.rusd().name();
			}
			else if (tokenAddr.equalsIgnoreCase( Monitor.chain().busd().address() ) ) {
				ret = Monitor.m_config.busd().name();
			}
		}
		catch( Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}
