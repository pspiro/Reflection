package monitor.wallet;
import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;

/** Panel to display blockchain transactions with related transactions consolidated
 *  into a single row */
public class BlockSummaryPanel extends BlockPanelBase {
	
	private JsonModel m_model = new JsonModel( JsonModel.getAllFields( Transfer.class) );

	public BlockSummaryPanel() {
		super();
		
		m_model.justify("llrlr");
		
		
		JLabel lab = new JLabel("Blockchain Transactions");
		lab.setBorder( new EmptyBorder( 2, 3, 2, 0) );
		add(lab, BorderLayout.NORTH);

		add( m_model.createTable() );
	}

	public void refresh( String walletAddr, Transfers ts) throws Exception {
		m_model.ar().clear();
		
		// create a map of transaction hash to a list of Transfers having that transaction hash
		HashMap<String,Transfers> map = new HashMap<>();
		for (var obj : ts) {
			Util.getOrCreate( map, obj.hash(), () -> new Transfers() )
				.add( obj);
		}
		
		// key is trans hash, val is list of transactions with that hash
		Util.forEach( map, (key,val) -> {
			if (val.size() == 1) {
				tryIt( val.get(0) );
			}
			else if (val.size() == 2) {
				Transfer t1 = val.get(0);
				Transfer t2 = val.get(1);
				
				if (!tryIt( t1, t2) ) {
					tryIt( t2, t1);
				}
			}
		});
		
		m_model.ar().sortJson( "time", true);
		m_model.fireTableDataChanged();
	}

	/** single transaction */
	private void tryIt(Transfer trans) {
		JsonObject obj = new JsonObject();
		// you have to look this up. pas obj.put("time", trans.getString( timestamp)) ;
		obj.put("qty", trans.amount() );
		// you have to look this up. pas obj.put("token", trans.getString( tokenSymbol) );

		String action;
		if (isMint( trans) ) {
			action = "Mint";
		}
		else if (isBurn( trans) ) {
			action = "Burn";
		}
		else if (trans.from().equals( Me) ) {
			action = "Send";
		}
		else if (trans.to().equals( Me) ) {
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
	private boolean tryIt(Transfer trans1, Transfer trans2) throws Exception {
		JsonObject obj = new JsonObject();
		obj.put("time", "???"); //trans1.getString( timestamp)) ;

		// redeem RUSD for USDT?
		if (isBusd( trans1) &&  
			trans1.from().equalsIgnoreCase(config().refWalletAddr()) &&
			isBurn( trans2) && isRusd( trans2) ) {

			obj.put("action", "Redeem");
			obj.put("qty", trans1.amount() );
			//obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.amount() );
			//obj.put("stablecoin", trans2.getString( tokenSymbol) );

			m_model.ar().add( obj);
			
			return true;
		}

		// buy stock with RUSD or USDT?
		if (isMint( trans1) && isStock( trans1) &&
			(isBurn( trans2) && isRusd( trans2) ||
			 isBusd( trans2) && isFromMe( trans2) && isToRefWallet( trans2) ) ) {

			obj.put("action", "Buy");
			obj.put("qty", trans1.amount() );
//			obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.amount() );
//			obj.put("stablecoin", trans2.getString( tokenSymbol) );
			
			m_model.ar().add( obj);

			return true;
		}
			
		// sell stock for RUSD?
		if (isMint( trans1) && isRusd( trans1) &&
			isBurn( trans2) && isStock( trans2) ) {
			
			obj.put("action", "Sell" );
			obj.put("qty", trans2.amount() );
//			obj.put("token", trans2.getString( tokenSymbol) );
			obj.put("amount", trans1.amount() );
//			obj.put("stablecoin", trans1.getString( tokenSymbol) );
				
			m_model.ar().add( obj);

			return true;
		}
		
		if (isBurn( trans1) && isRusd( trans1) && 
			isBusd( trans2) && isFromRefWallet( trans2) && isToMe( trans2) ) {
			obj.put("action", "Redeem" );
			obj.put("qty", trans1.amount() );
//			obj.put("token", trans1.getString( tokenSymbol) );
			obj.put("amount", trans2.amount() );
//			obj.put("stablecoin", trans2.getString( tokenSymbol) );

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
