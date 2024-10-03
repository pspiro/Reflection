package monitor.wallet;

import static monitor.Monitor.m_config;

import java.util.HashMap;

import org.json.simple.JsonArray;

import common.JsonModel;
import common.Util;
import web3.NodeInstance;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;

/** Panel to display all blockchain transactions for the tokens we care about */
public class BlockDetailPanel extends BlockPanelBase {
	
	private HashMap<String,String> commonMap = new HashMap<>(); // map wallet address (lower case) to wallet name
	
	private JsonModel m_model = new Model();
	
	class Model extends JsonModel {
		Model() {
			super( JsonModel.getAllFields( Transfer.class) );
		}
		
		@Override protected void onDoubleClick(String tag, Object val) {
			if (tag.equals( "transaction_hash") ) {
				Util.browse( m_config.blockchainTx( val.toString() ) );
			}				
		}
	}

	public BlockDetailPanel() {
		
		// you can filter on "possible_spam" if desired
		m_model.justify("lllr");
		
		add( m_model.createTable() );
		
		Util.wrap( () -> {
			commonMap.put( refWallet, RefWallet);
			commonMap.put( m_config.admin1Addr().toLowerCase(), "Admin1");
			commonMap.put( m_config.ownerAddr().toLowerCase(), "Owner");
			commonMap.put( NodeInstance.prod, "My prod wallet");
		});
	}

	public void refresh( String walletAddr, Transfers ts) throws Exception {
		m_model.ar().clear();
		
		if (Util.isValidAddress(walletAddr) ) {

			m_model.setRows( JsonArray.toJson( ts) );
			
			// adjust timestamp
			m_model.ar().update( timestamp, val -> val.toString().replace( "T", "  ").replace( "Z", "") );
			
			// round value to two places
			//m_model.ar().update( valueDecimal, val -> S.fmt2( Util.toDouble( val) ) );
			
			// add "Mint" and "Burn"
			m_model.ar().update( fromAddress, val -> val.equals( nullAddr) ? "Mint" : val);
			m_model.ar().update( toAddress, val -> val.equals( nullAddr) ? "Burn" : val);
			
			// label selected wallet with "***"
			m_model.ar().update( fromAddress, val -> ((String)val).equalsIgnoreCase( walletAddr) ? Me : val);  // it not efficient to loop twice
			m_model.ar().update( toAddress, val -> ((String)val).equalsIgnoreCase( walletAddr) ? Me : val);
			
			// replace system wallet address with wallet name, e.g. RefWallet
			m_model.ar().update( fromAddress, val -> Util.valOr( commonMap.get( (String)val), (String)val) );
			m_model.ar().update( toAddress, val -> Util.valOr( commonMap.get( (String)val), (String)val) );
		};
		
		m_model.ar().sortJson( timestamp, true);
		m_model.fireTableDataChanged();
	}
	
	/** These rows are used to feed into the consolidated blockchain panel */
	public JsonArray rows() {
		return m_model.ar();
	}

}
