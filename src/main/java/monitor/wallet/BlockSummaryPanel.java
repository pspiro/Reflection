package monitor.wallet;
import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import fireblocks.Accounts;

/** Panel to display blockchain transactions with related transactions consolidated
 *  into a single row */
public class BlockSummaryPanel extends BlockPanelBase {
	
	private JsonModel m_model = new JsonModel("time,action,qty,token,amount,stablecoin");

	public BlockSummaryPanel() {
		super();
		
		m_model.justify("llrlr");
		
		
		JLabel lab = new JLabel("Blockchain Transactions");
		lab.setBorder( new EmptyBorder( 2, 3, 2, 0) );
		add(lab, BorderLayout.NORTH);

		add( m_model.createTable() );
	}

	public void refresh( String walletAddr, JsonArray allRows) throws Exception {
		m_model.ar().clear();
		
		// create a map of transaction hash to a JsonArray of objects having that transaction hash
		HashMap<String,JsonArray> map = new HashMap<>();
		allRows.forEach( obj -> Util.getOrCreate( map, obj.getString("transaction_hash"), () -> new JsonArray() ).add( obj) ); // confused much?
		
		// key is trans hash, val is array of transactions with that hash
		Util.forEach( map, (key,val) -> {
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
		
		m_model.ar().sortJson( "time", true);
		m_model.fireTableDataChanged();
	}

	/** single transaction */
	private void tryIt(JsonObject trans) {
		JsonObject obj = new JsonObject();
		obj.put("time", trans.getString( timestamp)) ;
		obj.put("qty", trans.getString( valueDecimal) );
		obj.put("token", trans.getString( tokenSymbol) );

		String action;
		if (isMint( trans) ) {
			action = "Mint";
		}
		else if (isBurn( trans) ) {
			action = "Burn";
		}
		else if (trans.getString( fromAddress).equals( Me) ) {
			action = "Send";
		}
		else if (trans.getString( toAddress).equals( Me) ) {
			action = "Receive";
		}
		else {
			action = "Unknown";
			obj.display();
		}
		
		obj.put( "action", action);
		
		m_model.ar().add( obj);
	}

	/** Pair of transactions */
	private boolean tryIt(JsonObject trans1, JsonObject trans2) throws Exception {
		JsonObject obj = new JsonObject();
		obj.put("time", trans1.getString( timestamp)) ;

		// redeem RUSD for USDT?
		if (isUsdt( trans1) &&  
			trans1.getString(fromAddress).equalsIgnoreCase(Accounts.instance.getAddress("RefWallet") ) &&
			isBurn( trans2) && isRusd( trans2) ) {

			obj.put("action", "Redeem");
			obj.put("qty", trans1.getString( valueDecimal) );
			obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.getString( valueDecimal) );
			obj.put("stablecoin", trans2.getString( tokenSymbol) );

			m_model.ar().add( obj);
			
			return true;
		}

		// buy stock with RUSD or USDT?
		if (isMint( trans1) && isStock( trans1) &&
			(isBurn( trans2) && isRusd( trans2) ||
			 isUsdt( trans2) && isFromMe( trans2) && isToRefWallet( trans2) ) ) {

			obj.put("action", "Buy");
			obj.put("qty", trans1.getString( valueDecimal) );
			obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.getString( valueDecimal) );
			obj.put("stablecoin", trans2.getString( tokenSymbol) );
			
			m_model.ar().add( obj);

			return true;
		}
			
		// sell stock for RUSD?
		if (isMint( trans1) && isRusd( trans1) &&
			isBurn( trans2) && isStock( trans2) ) {
			
			obj.put("action", "Sell" );
			obj.put("qty", trans2.getString( valueDecimal) );
			obj.put("token", trans2.getString( tokenSymbol) );
			obj.put("amount", trans1.getString( valueDecimal) );
			obj.put("stablecoin", trans1.getString( tokenSymbol) );
				
			m_model.ar().add( obj);

			return true;
		}
		
		if (isBurn( trans1) && isRusd( trans1) && 
			isUsdt( trans2) && isFromRefWallet( trans2) && isToMe( trans2) ) {
			obj.put("action", "Redeem" );
			obj.put("qty", trans1.getString( valueDecimal) );
			obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.getString( valueDecimal) );
			obj.put("stablecoin", trans2.getString( tokenSymbol) );

			m_model.ar().add( obj);

			return true;
		}			
		
		return false;
	}

	public void clear() {
		m_model.ar().clear();
		m_model.fireTableDataChanged();
	}
}
