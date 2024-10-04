package monitor;

import static monitor.Monitor.m_config;
import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;

import javax.swing.JLabel;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.MySqlConnection;
import tw.util.S;
import web3.Erc20;

/** Shows the holders for a given token (wallet and balance */
public class HoldersPanel extends JsonPanel {
	public static Format FMT = new DecimalFormat( "#,##0.0000");
	private JLabel m_title = new JLabel();
	
	HoldersPanel() {
		super( new BorderLayout(), "wallet,name,balance");
		add( m_title, BorderLayout.NORTH);
		add( m_model.createTable() );
	}

	@Override protected void refresh() throws Exception {
	}
	
	/** show four decimals */
	@Override protected Object format(String key, Object value) {
		return value instanceof Double ? S.fmt4((double)value) : value; 
	}

	@Override protected void onDouble(String tag, Object val) {
		if (S.notNull(tag).equals("wallet") ) {
			Monitor.m_tabs.select("Wallet");
			Monitor.m_walletPanel.setWallet(val.toString());
		}
	}
	
	// there's a bug in the Moralis code; the same transaction gets returned twice;
	// to fix it, look at the transaction_hash field and filter out the dups
	// see email to Moralis on 6/9/24
	
	double total;
	public void refresh(Erc20 token) {  // the decimal is wrong here, that's why rusd doesn't work
		wrap( () -> {
			m_title.setText( token.name() );
			
			var map = m_config.node().getHolderBalances( token.address(), token.decimals() );

			JsonArray ar = new JsonArray();
			
			total = 0;
			
			String[] wallets = map.keySet().toArray( new String[0]);
			String list = String.format( "'" + String.join( "','", wallets) + "'");
			String query = String.format( "select wallet_public_key, first_name, last_name from users where wallet_public_key in (%s)", list).toLowerCase();
			S.out( query);
			
			JsonArray names = m_config.sqlQuery(query);
			HashMap<String,JsonObject> nameMap = names.getMap( "wallet_public_key"); 
			
			Util.forEach( map, (wallet, balance) -> { 
				if (balance >= .0001) {
					ar.add( Util.toJson( 
							"wallet", wallet,
							"name", getName( nameMap.get(wallet) ),
							"balance", balance ) );
					total += balance;
				}
			});
			
			setRows( ar);
			m_model.fireTableDataChanged();
			
			m_title.setText( S.format( "%s  Total: %s", token.name(), total) ); 
		});
	}

	private String getName(JsonObject obj) {
		return obj != null 
				? String.format( "%s %s", obj.getString( "first_name"), obj.getString( "last_name"))
				: null;
	}

	private String getUsersName(MySqlConnection sql, String wallet) throws Exception {
		S.out( "querying for name for %s", wallet);
		JsonObject obj = sql.querySingleRecord(
				"select first_name,last_name from users where wallet_public_key = '%s'", 
				wallet.toLowerCase() );
		return obj != null ? String.format( "%s %s", obj.getString("first_name"), obj.getString("last_name") ) : "";
	}

}
