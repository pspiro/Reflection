package monitor;

import java.awt.event.ActionListener;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.S;
import tw.util.UI;

class UsersPanel extends QueryPanel {
	static String names = "created_at,wallet_public_key,first_name,last_name,locked_until,email,kyc_status,phone,aadhaar,pan_number,address,city,country,id,persona_response";
	static String sql = "select * from users $where";
	
	UsersPanel() {
		super( "users", names, sql);
	}
	
	@Override public void adjust(JsonObject obj) {
		obj.update( "first_name", name -> Util.initialCap( name.toString() ) );
		obj.update( "last_name", name -> Util.initialCap( name.toString() ) );
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
	
	@Override protected void buildMenu(JPopupMenu m, JsonObject rec, String tag, Object val) {
		m.add( JsonModel.menuItem("Show Wallet", ev -> {
			String wallet = rec.getString( "wallet_public_key");
			if (Util.isValidAddress( wallet) ) {
				Monitor.m_tabs.select("Wallet");
				Monitor.m_walletPanel.setWallet( wallet);
			}
		}));
		m.add( JsonModel.menuItem("Verify",  ev -> verify( rec) ) );
		m.add( JsonModel.menuItem("Delete", ev -> delete( rec) ) );
	}
	
	private ActionListener verify(JsonObject rec) {
		if (Util.confirm( this, "Are you sure you want to verify this user?") ) {
			try {
				Monitor.m_config.sqlCommand( sql -> sql.updateJson( 
					"users",  
					Util.toJson( "kyc_status", "VERIFIED"),
					"wallet_public_key = '%s'", 
					rec.getString("wallet_public_key").toLowerCase() ) );
			}
			catch( Exception e) {
				e.printStackTrace();
				Util.inform( this, "Error - " + e.getMessage() );
			}
		}
		return null;
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
	
	static class PersonaPanel extends QueryPanel {
		PersonaPanel() {
			super( "users", 
				   "wallet_public_key,first_name,last_name,email,persona_name,persona_id,birthdate,country",
				   "select * from users $where");
			where.setText( "where persona_response <> ''");
		}
		
		@Override public void adjust(JsonObject obj) {
			obj.update( "first_name", name -> Util.initialCap( name.toString() ) );
			obj.update( "last_name", name -> Util.initialCap( name.toString() ) );
			
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
