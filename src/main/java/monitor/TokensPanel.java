package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

public class TokensPanel extends JsonPanel {
	HashMap<Integer,JsonObject> m_map = new HashMap<>(); // map conid to record, key is Integer 

	TokensPanel() {
		super( new BorderLayout(), "symbol,conid,smartcontractid,tokens,position,dif,isHot");
		add( m_model.createTable() );
		m_model.justify("lllrr");
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing Tokens panel");
		
		m_model.m_ar.clear();
		m_map.clear();
		
		// start with the stocks from the spreadsheet and add each to the map
		// this shows active stocks only
		Monitor.stocks.stocks().forEach( stock -> {
			m_model.m_ar.add(stock);
			m_map.put(stock.getInt("conid"), stock);
		});
		SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		
		// add IB positions
		MyClient.getArray(Monitor.refApiBaseUrl() + "/api/?msg=getpositions", positions -> {
			positions.forEach( position ->
				getOrCreate( position.getInt("conid") )
					.put( "position", position.get("position") )
			);
			SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
		});

		// add token supply (execute in a separate thread because it takes a while)
		// must iterate of stocks and not m_model.m_ar or you get ConcurModExc
		// note that if there are stocks for which we have an IB position but are not in the spreadsheet,
		// we won't query for the blockchain position
		Util.execute( () -> {
			Monitor.stocks.forEach( stock -> {
				Util.wrap( () -> {
					double supply = stock.queryTotalSupply();
					S.out( "Total supply for %s is %s", stock.getString("symbol"), supply);
					stock.put("tokens", supply);
					SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
				});
			});
		});
	}
	
	private JsonObject getOrCreate(int conid) {
		return Util.getOrCreate(m_map, conid, () -> {
			JsonObject obj = new JsonObject();
			obj.put("conid", String.valueOf(conid) );
			m_model.m_ar.add( obj);
			return obj;
		});
	}
}
