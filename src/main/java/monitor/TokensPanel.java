package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.StockToken;

/** Note that we read the list directly from the spreadsheet,
 *  so changes are not picked up real-time
 */
public class TokensPanel extends JsonPanel {
	private HashMap<Integer,JsonObject> m_map = new HashMap<>(); // map conid to record, key is Integer 
	private HoldersPanel m_holdersPanel = new HoldersPanel();
	
	TokensPanel() {
		super( new BorderLayout(), "symbol,conid,address,tokenSupply,position,dif");
		add( m_model.createTable() );
		add( m_holdersPanel, BorderLayout.EAST);
		m_model.justify("lllrr");
	}
	
	@Override protected void onDouble(String tag, Object val) {
		if (tag.equals( "smartcontractid")) {
			try {
				m_holdersPanel.refresh( new StockToken( val.toString(), config().chain() ) );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing Tokens panel");
		
		rows().clear();
		m_map.clear();
		
		// start with the stocks from the spreadsheet and add each to the map
		// this shows active stocks only
		Monitor.tokens().forEach( token -> {
			var row = Util.toJson( 
					"conid", token.conid(),
					"address", token.address() );
			rows().add( row);
					
			m_map.put( token.rec().conid(), row);
		});
		SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		
		// add IB positions
		MyClient.getArray(Monitor.refApiBaseUrl() + "/api/?msg=getpositions", positions -> {
			positions.forEach( position ->
				getOrCreateRow( position.getInt("conid") )
					.put( "position", position.get("position") )
			);
			SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		});

		// add token supply (execute in a separate thread because it takes a while)
		// must iterate of stocks and not m_model.m_ar or you get ConcurModExc
		// note that if there are stocks for which we have an IB position but are not in the spreadsheet,
		// we won't query for the blockchain position
		Util.execute( () -> {
			Monitor.tokens().forEach( token -> {
				wrap( () -> {
					double supply = token.queryTotalSupply();
					S.out( "Total supply for %s is %s", token.name(), supply);
					var row = getOrCreateRow( token.conid() );
					row.put("tokenSupply", supply);
					SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
				});
			});
		});
	}
	
	private JsonObject getOrCreateRow(int conid) {
		return Util.getOrCreate(m_map, conid, () -> {
			JsonObject obj = new JsonObject();
			obj.put("conid", String.valueOf(conid) );
			rows().add( obj);
			return obj;
		});
	}
}
