package monitor.wallet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import monitor.Monitor;
import tw.util.S;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;

/** Panel to display all blockchain transactions for the tokens we care about */
public class BlockDetailPanel extends BlockPanelBase {
	
	
	private JsonModel m_model = new Model();
	
	class Model extends JsonModel {
		Model() {
			super( JsonModel.getAllFields( Transfer.class) );
		}

		@Override protected void onCtrlClick(JsonObject row, String tag) {
			Util.wrap( () -> {
				int num = row.getInt( "block");
				if (num > 0) {
					row.put( 
							"timestamp", 
							Monitor.chain().node().getBlockDateTime(num) );
					fireTableDataChanged();
				}
			});
		}
		
		@Override protected void onDoubleClick(String tag, Object val) {
			switch( tag) {
				case "transaction_hash":
					Util.browse( Monitor.chain().browseTx( val.toString() ) );
					break;
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
		m_model.justify("llrlrlr");
		
		add( m_model.createTable() );
	}
	
	public void refresh( String walletAddr, Transfers ts) throws Exception {
		m_model.ar().clear();
		
		if (Util.isValidAddress(walletAddr) ) {

			m_model.setRows( JsonArray.toJson( ts) );
			
//			// adjust timestamp
//			m_model.ar().update( timestamp, val -> val.toString().replace( "T", "  ").replace( "Z", "") );
		};
		
		m_model.ar().sortJson( "block", true);
		m_model.fireTableDataChanged();
	}
	
	/** These rows are used to feed into the consolidated blockchain panel */
	public JsonArray rows() {
		return m_model.ar();
	}

}
