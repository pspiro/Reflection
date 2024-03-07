package monitor.wallet;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import fireblocks.Accounts;
import positions.MoralisServer;
import tw.util.DualPanel;
import tw.util.S;

/** Panel to display blockchain transactions */
public class BlockPanel extends DualPanel {
	private static final String timestamp = "block_timestamp";

	static final String nullAddr = "0x0000000000000000000000000000000000000000";
	
	private HashMap<String,String> map = new HashMap<>();
	
	private JsonModel m_mod1 = new JsonModel("block_timestamp,from_address,to_address,value_decimal,token_symbol,transaction_hash");
	private JsonModel m_mod2 = new JsonModel("time,action,qty,token,amount,stablecoin");
	
	public BlockPanel() {
		// you can filter on "possible_spam" if desired
		m_mod1.justify("lllr");
		m_mod2.justify("llrlr");
		
		add( "1", m_mod1.createTable() );
		add( "2", m_mod2.createTable() );
		
		Util.wrap( () -> {
			map.put( Accounts.instance.getAddress("RefWallet").toLowerCase(), "RefWallet");
			map.put( Accounts.instance.getAddress("Admin1").toLowerCase(), "Admin1");
			map.put( Accounts.instance.getAddress("Admin2").toLowerCase(), "Admin2");
			map.put( Accounts.instance.getAddress("Owner").toLowerCase(), "Owner");
			//map.put( nullAddr, "");
		});
	}

	void refresh( String walletAddr) throws Exception {
		refreshTab1( walletAddr);
		refreshTab2( walletAddr);
	}
	
	void refreshTab1( String walletAddr) throws Exception {
		m_mod1.ar().clear();
		
		if (Util.isValidAddress(walletAddr) ) {

			MoralisServer.getAllWalletTransfers( walletAddr, ar -> {
				m_mod1.ar().addAll( ar);
				ar.print();
			});
			
			// filter and update rows
			m_mod1.ar().filter( obj -> obj.getDouble("value_decimal") != 0);  // remove rows with value zero
			m_mod1.ar().update( "from_address", val -> val.equals( nullAddr) ? "Mint" : val);
			m_mod1.ar().update( "to_address", val -> val.equals( nullAddr) ? "Burn" : val);
			m_mod1.ar().update( timestamp, val -> val.toString().replace( "T", "  ") );
			m_mod1.ar().update( "value_decimal", val -> S.fmt2( Util.toDouble( val) ) );
			m_mod1.ar().update( "from_address", val -> ((String)val).equalsIgnoreCase( walletAddr) ? "Me" : val);  // it not efficient to loop twice
			m_mod1.ar().update( "to_address", val -> ((String)val).equalsIgnoreCase( walletAddr) ? "Me" : val);
		};
		
		m_mod1.ar().sortJson( timestamp, true);
		m_mod1.fireTableDataChanged();
	}
	
	void refreshTab2( String walletAddr) throws Exception {
		m_mod2.ar().clear();
		
		// create a map of transaction hash to a JsonArray of objects having that transaction hash
		HashMap<String,JsonArray> map = new HashMap<>();
		m_mod1.ar().forEach( obj -> Util.getOrCreate( map, obj.getString("transaction_hash"), () -> new JsonArray() ).add( obj) ); // confused much?
		
		// key is trans hash, val is array of transactions with that hash
		map.forEach( (key,val) -> {
			if (val.size() == 1) {
				tryIt( val.get(0) );
			}
			else if (val.size() == 2) {
				JsonObject t1 = val.get(0);
				JsonObject t2 = val.get(1);
				
				if (!tryIt( t1, t2) ) {
					tryIt( t2, t1);
				}
			}
		});
		
		m_mod2.ar().sortJson( timestamp, true);
		m_mod2.fireTableDataChanged();
	}

	private void tryIt(JsonObject t) {
		if (isMint( t) || isBurn( t) ) {
			JsonObject obj = new JsonObject();
			obj.put("time", t.getString( timestamp)) ;
			obj.put("action", isMint( t) ? "Mint" : "Burn");
			obj.put("qty", t.getString( "value_decimal") );
			obj.put("token", t.getString( "token_symbol") );
			
			m_mod2.ar().add( obj);
		}
	}
	
	private boolean tryIt(JsonObject mint, JsonObject burn) {
		if ( isMint( mint) && isBurn( burn) ) {	
			
			// buy stock?
			if (isStock( mint.getString( "token_symbol") ) && 
				burn.getString( "token_symbol").equals( "RUSD") ) {
			
				JsonObject obj = new JsonObject();
				obj.put("time", mint.getString( timestamp)) ;
				obj.put("action", "Buy" );
				obj.put("qty", mint.getString( "value_decimal") );
				obj.put("token", mint.getString( "token_symbol") );
				obj.put("amount", burn.getString( "value_decimal") );
				obj.put("stablecoin", burn.getString( "token_symbol") );
				
				m_mod2.ar().add( obj);
			}
			
			// sell stock
			else if (mint.getString( "token_symbol").equals( "RUSD") &&
				isStock( burn.getString( "token_symbol") ) ) { 
			
				JsonObject obj = new JsonObject();
				obj.put("time", mint.getString( timestamp)) ;
				obj.put("action", "Sell" );
				obj.put("qty", burn.getString( "value_decimal") );
				obj.put("token", burn.getString( "token_symbol") );
				obj.put("amount", mint.getString( "value_decimal") );
				obj.put("stablecoin", mint.getString( "token_symbol") );
				
				m_mod2.ar().add( obj);
			}
			return true;
		}
		
		return false;
	}

	private boolean isMint(JsonObject obj) {
		return obj.getString( "from_address").equals( "Mint");
	}

	private boolean isBurn(JsonObject obj) {
		return obj.getString( "to_address").equals( "Burn");
	}

	private boolean isStock(String string) {
		return string.endsWith( ".r");
	}

}
