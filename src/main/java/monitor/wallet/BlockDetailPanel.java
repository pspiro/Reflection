package monitor.wallet;

import static monitor.Monitor.m_config;

import org.json.simple.JsonArray;

import common.JsonModel;
import common.Util;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;

/** Panel to display all blockchain transactions for the tokens we care about */
public class BlockDetailPanel extends BlockPanelBase {
	
	
	private JsonModel m_model = new Model();
	
	class Model extends JsonModel {
		Model() {
			super( JsonModel.getAllFields( Transfer.class) );
		}
		
		@Override protected void onDoubleClick(String tag, Object val) {
			if (tag.equals( "transaction_hash") ) {
				Util.browse( m_config.chain().blockchainTx( val.toString() ) );
			}				
		}
		
//		@Override protected Object format(String key, Object value) {
//			if (key.equals( "amount") && value instanceof Double ) {
//				return S.fmt3( (double)value);
//			}
//			return value;
//		}
	}

	public BlockDetailPanel() {
		
		// you can filter on "possible_spam" if desired
		m_model.justify("lllr");
		
		add( m_model.createTable() );
	}
	
	public void refresh( String walletAddr, Transfers ts) throws Exception {
		m_model.ar().clear();
		
		if (Util.isValidAddress(walletAddr) ) {

			m_model.setRows( JsonArray.toJson( ts) );
			
			// adjust timestamp
			m_model.ar().update( timestamp, val -> val.toString().replace( "T", "  ").replace( "Z", "") );
		};
		
		m_model.ar().sortJson( timestamp, true);
		m_model.fireTableDataChanged();
	}
	
	/** These rows are used to feed into the consolidated blockchain panel */
	public JsonArray rows() {
		return m_model.ar();
	}

}
