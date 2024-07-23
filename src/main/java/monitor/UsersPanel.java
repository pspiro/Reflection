package monitor;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.S;
import tw.util.UI;

class UsersPanel extends QueryPanel {
	static String names = "created_at,wallet_public_key,first_name,last_name,persona_name,locked_until,email,kyc_status,phone,aadhaar,pan_number,persona_id,address,city,country,id,persona_response";
	static String sql = "select * from users $where";
	
	UsersPanel() {
		super( "users", names, sql);
	}
	
	@Override public void adjust(JsonObject userRec) {
		userRec.update( "first_name", name -> Util.initialCap( name.toString() ) );
		userRec.update( "last_name", name -> Util.initialCap( name.toString() ) );
		
		Util.wrap( () -> {
			JsonObject fields = userRec
					.getObjectNN( "persona_response")
					.getObjectNN("fields");
			userRec.put( "persona_name", String.format( "%s %s", getVal( fields, "name-first"), getVal( fields, "name-last") ));
			userRec.put( "persona_id", getVal( fields, "identification-number"));
		});
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
		m.add( JsonModel.menuItem("Delete", ev -> delete( rec) ) );
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
				   "wallet_public_key,first_name,last_name,kyc_status,persona_status,email,persona_name,persona_id,birthdate,country",
				   "select * from users $where");
			where.setText( "where persona_response <> ''");
		}
		
		@Override protected void buildMenu(JPopupMenu menu, JsonObject rec, String tag, Object val) {
			menu.add( JsonModel.menuItem("Show in Persona", ev -> {
				wrap( () -> {
					Util.iff( rec.getObject( "persona_response"), persona -> {
						Util.iff( persona.getString( "inquiryId"), id -> {
							Util.browse( "https://app.withpersona.com/dashboard/inquiries/" + id);
						});
					});
				});
			}));
		}
		
		@Override public void adjust(JsonObject obj) {
			obj.update( "first_name", name -> Util.initialCap( name.toString() ) );
			obj.update( "last_name", name -> Util.initialCap( name.toString() ) );
			
			// add fields from the persona response
			Util.wrap( () -> {
				JsonObject persona = obj.getObject( "persona_response");
				if (persona != null) {
					obj.put( "persona_status", persona.getString( "status") ); // status from persona response
					
					JsonObject fields = persona.getObject("fields");
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
	
	static String getVal( JsonObject fields, String tag) throws Exception {
		return fields.getObject( tag).getString( "value");
	}
}
