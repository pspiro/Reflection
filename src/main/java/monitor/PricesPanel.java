package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import http.MyHttpClient;
import monitor.Monitor.RefPanel;
import tw.util.MyTable;
import tw.util.S;

public class PricesPanel extends JPanel implements RefPanel {
	final Model m_mod = new Model();

	PricesPanel() {
		super( new BorderLayout() );

		add( new MyTable( m_mod).scroll() );
	}

	void initialize() {
		m_mod.initialize();
	}

	@Override public void refresh() throws Exception {
		m_mod.refresh();
	}

	class Model extends JsonModel {
		Model() {
			super( "symbol,conid,bid,ask,last,time,Real Bid,Real Ask");
		}
		
		void initialize() {
			Monitor.stocks.stockSet().forEach( stockIn -> {
				JsonObject stock = new JsonObject();
				stock.put( "symbol", stockIn.getSymbol() );
				stock.put( "conid", stockIn.getConid() );
				m_ar.add( stock);
			});
			
			m_mod.fireTableDataChanged();
		}

		void refresh() {
			try {
				MyHttpClient cli = new MyHttpClient("localhost", 8383);
				JsonArray ar = cli.get( "/api/?msg=getallprices").readJsonArray();
				ar.forEach( refPrices -> update(refPrices) ); 
			} catch (Exception e) {
				e.printStackTrace();
			}

			fireTableDataChanged();
		}

		private void update(JsonObject refPrices) {
			JsonObject stock = findStock(refPrices);
			if (stock != null) {
				stock.put( "bid", refPrices.getDouble("bid") );
				stock.put( "ask", refPrices.getDouble("ask") );
				stock.put( "last", refPrices.getDouble("last") );
				stock.put( "time", refPrices.getDouble("time") );
			}
			else {
				S.out( "Error: received refPrices " + refPrices);
			}
		}

		JsonObject findStock(JsonObject refPrices) {
			int conid = refPrices.getInt("conid");
			for (JsonObject stock : m_ar) {
				if (stock.getInt("conid") == conid) {
					return stock;
				}
			}
			return null;
		}
	}
	
	
	@Override public void activated() {
		m_mod.refresh();
	}

	@Override public void closed() {
	}

}
