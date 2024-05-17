package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JLabel;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import reflection.MySqlConnection;
import tw.util.S;

/** Shows the holders for a given token (wallet and balance */
public class HoldersPanel extends JsonPanel {
	private JLabel m_title = new JLabel();
	
	HoldersPanel() {
		super( new BorderLayout(), "wallet,name,balance");
		add( m_title, BorderLayout.NORTH);
		add( m_model.createTable() );
	}

	@Override protected void refresh() throws Exception {
	}

	@Override protected void onDouble(String tag, Object val) {
		if (S.notNull(tag).equals("wallet") ) {
			Monitor.m_tabs.select("Wallet");
			Monitor.m_walletPanel.setWallet(val.toString());
		}
	}
	
	double total;
	public void refresh(Erc20 token) {  // the decimal is wrong here, that's why rusd doesn't work
		wrap( () -> {
			m_title.setText( token.name() );
			
			HashMap<String,Double> map = token.getAllBalances();

			JsonArray ar = new JsonArray();
			
			total = 0;

			Monitor.m_config.sqlCommand( sql -> {  // make all username queries from a single database connection
				Util.forEach( map, (wallet, balance) -> { 
					if (balance >= .009) {
						ar.add( Util.toJson( 
								"wallet", wallet,
								"name", getUsersName(sql, wallet),
								"balance", balance ) );
						total += balance;
					}
				});
			});
			
			setRows( ar);
			m_model.fireTableDataChanged();
			
			m_title.setText( S.format( "%s  Total: %s", token.name(), total) ); 
		});
	}

	private String getUsersName(MySqlConnection sql, String wallet) throws Exception {
		S.out( "querying for name for %s", wallet);
		JsonObject obj = sql.querySingleRecord(
				"select first_name,last_name from users where wallet_public_key = '%s'", 
				wallet.toLowerCase() );
		return obj != null ? String.format( "%s %s", obj.getString("first_name"), obj.getString("last_name") ) : "";
	}

}
