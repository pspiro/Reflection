package monitor;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.json.simple.JsonObject;

import http.MyHttpClient;
import monitor.Monitor.RefPanel;
import tw.util.MyTable;
import tw.util.S;

public class PricesPanel extends JPanel implements RefPanel {
	final Model m_model = new Model();

	PricesPanel() {
		super( new BorderLayout() );

		add( m_model.createTable() );
	}

	void initialize() {
		m_model.initialize();
	}

	@Override public void refresh() throws Exception {
		m_model.refresh();
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
			
			m_model.fireTableDataChanged();
		}

		void refresh() {
			try {
				MyHttpClient cli = new MyHttpClient("localhost", 8383);
				JsonObject map = cli.get( "/api/?msg=getallprices").readJsonObject();
				//JsonArray ar = cli.get( "/api/?msg=get-stocks-with-prices").readJsonArray();
				map.forEach( (conid,prices) -> update(Integer.parseInt(conid), (JsonObject)prices) ); 
			} catch (Exception e) {
				e.printStackTrace();
			}

			fireTableDataChanged();
		}

		private void update(int conid, JsonObject refPrices) {
			JsonObject stock = findStock(conid);
			if (stock != null) {
				stock.put( "bid", refPrices.getDouble("bid") );
				stock.put( "ask", refPrices.getDouble("ask") );
				stock.put( "last", refPrices.getDouble("last") );
				stock.put( "time", refPrices.getString("time") );
			}
			else {
				S.out( "Error: received prices for unknown conid " + conid);
			}
		}

		JsonObject findStock(int conid) {
			for (JsonObject stock : m_ar) {
				if (stock.getInt("conid") == conid) {
					return stock;
				}
			}
			return null;
		}
	}
	
	
	@Override public void activated() {
		m_model.refresh();
	}

	@Override public void closed() {
	}

}
