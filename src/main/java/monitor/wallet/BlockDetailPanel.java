package monitor.wallet;

import java.util.HashMap;

import org.json.simple.JsonArray;

import common.JsonModel;
import common.Util;
import monitor.Monitor;
import tw.util.S;
import web3.MoralisServer;

/** Panel to display all blockchain transactions for the tokens we care about */
public class BlockDetailPanel extends BlockPanelBase {
	
	private HashMap<String,String> commonMap = new HashMap<>(); // map wallet address (lower case) to wallet name
	
	private JsonModel m_model = new Model();
	
	class Model extends JsonModel {
		Model() {
			super( "block_timestamp,from_address,to_address,value_decimal,token_symbol,transaction_hash");
		}
		
		@Override protected void onDoubleClick(String tag, Object val) {
			if (tag.equals( "transaction_hash") ) {
				Util.browse( Monitor.m_config.blockchainTx( val.toString() ) );
			}				
		}
	}
	
	public BlockDetailPanel() {
		
		// you can filter on "possible_spam" if desired
		m_model.justify("lllr");
		
		add( m_model.createTable() );
		
		Util.wrap( () -> {
			commonMap.put( refWallet, RefWallet);
			commonMap.put( Monitor.m_config.admin1Addr().toLowerCase(), "Admin1");
			//commonMap.put( Monitor.m_config.admin1Addr().toLowerCase(), "Admin2");
			commonMap.put( Monitor.m_config.ownerAddr().toLowerCase(), "Owner");
			commonMap.put( "0x2703161d6dd37301ced98ff717795e14427a462b", "My prod wallet");
			//map.put( nullAddr, "");
		});
	}

	void refresh( String walletAddr) throws Exception {
		m_model.ar().clear();
		
		if (Util.isValidAddress(walletAddr) ) {

			MoralisServer.getAllWalletTransfers( walletAddr, ar -> {
				m_model.ar().addAll( ar);
				ar.print();
			});

			// sometimes the decimals and value_decimal are null so it show as zero size
			
			// filter and update rows
			m_model.ar().filter( obj -> obj.getDouble("value") != 0 && weCare( obj) );  // remove rows with value zero
			m_model.ar().update( timestamp, val -> val.toString().replace( "T", "  ").replace( "Z", "") );
			m_model.ar().update( valueDecimal, val -> S.fmt2( Util.toDouble( val) ) );
			m_model.ar().update( fromAddress, val -> val.equals( nullAddr) ? "Mint" : val);
			m_model.ar().update( toAddress, val -> val.equals( nullAddr) ? "Burn" : val);
			m_model.ar().update( fromAddress, val -> ((String)val).equalsIgnoreCase( walletAddr) ? Me : val);  // it not efficient to loop twice
			m_model.ar().update( toAddress, val -> ((String)val).equalsIgnoreCase( walletAddr) ? Me : val);
			m_model.ar().update( fromAddress, val -> Util.valOr( commonMap.get( (String)val), (String)val) );
			m_model.ar().update( toAddress, val -> Util.valOr( commonMap.get( (String)val), (String)val) );
		};
		
		m_model.ar().sortJson( timestamp, true);
		m_model.fireTableDataChanged();
	}
	
	/** These rows are used to feed into the consolidated blockchain panel */
	JsonArray rows() {
		return m_model.ar();
	}

}
