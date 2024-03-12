package monitor;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.S;
import tw.util.UI;

class UsersPanel extends QueryPanel {
	static String names = "created_at,wallet_public_key,first_name,last_name,locked_until,email,kyc_status,phone,aadhaar,address,city,country,id,pan_number,persona_response";
	static String sql = "select * from users $where";
	
	UsersPanel() {
		super( "users", names, sql);
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

	@Override protected void buildMenu(JPopupMenu m, JsonObject record, String tag, Object val) {
		m.add( JsonModel.menuItem("Delete", ev -> delete( record) ) );
	}
	
	void delete(JsonObject rec) {
		// confirm
		if (Util.confirm( this, "Are you sure you want to delete this record?") ) {
			try {
				Monitor.m_config.sqlCommand( sql -> {
					if (sql.delete( "delete from users where wallet_public_key = '%s'",
							rec.getString("wallet_public_key").toLowerCase() ) > 0) {
						
						UI.flash( "Record deleted");
						m_model.ar().remove( rec);
						m_model.fireTableDataChanged();
					}
				});
			}
			catch( Exception e) {
				e.printStackTrace();
				Util.inform( this, "Error - " + e.getMessage() );
			}
		}
	}
}
