package monitor;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.S;

class UsersPanel extends QueryPanel {
	static String names = "created_at,wallet_public_key,first_name,last_name,locked_until,email,kyc_status,phone,aadhaar,address,city,country,id,pan_number,persona_response";
	static String sql = "select * from users $where";
	
	UsersPanel() {
		super( "users", names, sql);
	}
	
	@Override protected void onRightClick(MouseEvent e, JsonObject rec, String tag, Object val) {
		JPopupMenu m = new JPopupMenu();
		m.add( JsonModel.menuItem("Copy", ev -> Util.copyToClipboard(val) ) );
		m.add( JsonModel.menuItem("Show Wallet", ev -> {
			String wallet = rec.getString( "wallet_public_key");
			if (Util.isValidAddress( wallet) ) {
				Monitor.m_tabs.select("Wallet");
				Monitor.m_walletPanel.setWallet( wallet);
			}
		}));
		//m.add( JsonModel.menuItem("Delete", ev -> delete( record) ) );
		m.show( e.getComponent(), e.getX(), e.getY() );
	}

	@Override protected String getTooltip(JsonObject row, String tag) {
		String ret = null;

		if (tag.equals("persona_response")) {
			try {
				ret = JsonObject.parse( row.getString(tag) ).getObject("fields").toHtml();
			} catch (Exception e) {
				// eat it
			}
		}
		return ret;
	}
	
	// there's one slight problem here; if they haven't opened the Wallet panel yet
	// it going to activate it, which will refresh it, then refresh it again
	// with the values passed in
	
	// you have to not activate it, but mark it as activated so it doesn't
	// refresh if they click on it later
	@Override protected void onDouble(String tag, Object val) {
		if (S.notNull(tag).equals("wallet_public_key") ) {
			Monitor.m_tabs.select("Wallet");
			Monitor.m_walletPanel.setWallet(val.toString());
		}
	}
	
	static class PersonaPanel extends QueryPanel {
		PersonaPanel() {
			super( "users", 
				   "wallet_public_key,first_name,last_name,email,persona_name,persona_id,birthdate,country",
				   "select * from users $where");
			where.setText( "where persona_response <> ''");
		}
		
		@Override public void adjust(JsonObject obj) {
			Util.wrap( () -> {
				String persona = obj.getString( "persona_response");
				if (JsonObject.isObject( persona) ) {
					JsonObject fields = JsonObject.parse( persona).getObject("fields");
					obj.put( "persona_name", String.format( "%s %s", getVal( fields, "name-first"), getVal( fields, "name-last") ));
					obj.put( "birthdate", getVal( fields, "birthdate") );
					obj.put( "country", getVal( fields, "address-country-code") );
					obj.put( "persona_id", getVal( fields, "identification-number"));
				}
			});
		}
		
		@Override protected void onDouble(String tag, Object val) {
			if (S.notNull(tag).equals("wallet_public_key") ) {
				Monitor.m_tabs.select("Wallet");
				Monitor.m_walletPanel.setWallet(val.toString());
			}
		}

	}
	
	static String getVal( JsonObject obj, String tag) throws Exception {
		return obj.getObject( tag).getString( "value");
	}
}
