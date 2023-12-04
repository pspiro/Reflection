package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

public class PricesPanel extends JsonPanel {
	final static String AlphaKey = "EKD9A4ZUSQPEPFXK";  // alphavantage API key
	final static String AlphaUrl = "https://www.alphavantage.co";
	
	PricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,time");
		add( m_model.createTable() );
		m_model.justify("llrrr");
	}
	
	@Override JsonModel createModel(String allNames) {
		return new Model(allNames);
	}

	@Override public void activated() {
		S.out( "Initializing Prices panel");
		((Model)m_model).initialize();
	}

	@Override protected Object format(String key, Object value) {
		return key.equals("bid") || key.equals("ask") || key.equals("last")
				? MdsPricesPanel.fmtPrice(value) 
				: value;
	}

	class Model extends JsonModel {
		Model(String allNames) {
			super(allNames);
		}
		
		/** Called at startup */
		void initialize() {
			Monitor.stocks.stockSet().forEach( stockIn -> {
				JsonObject stock = new JsonObject();
				stock.put( "symbol", stockIn.symbol() );
				stock.put( "conid", stockIn.conid() );
				m_ar.add( stock);
			});
			
			m_model.fireTableDataChanged();
		}

		void refresh() throws Exception {
			super.refresh();
			MyClient.getJson(Monitor.refApiBaseUrl() + "/api/?msg=getallprices", map -> {
				map.forEach( (conid,prices) -> update(Integer.parseInt(conid), (JsonObject)prices) ); 
				fireTableDataChanged();
			});
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
		
		@Override public void onLeftClick(MouseEvent event, int row, int col) {
			JsonObject stock = getRow(row);
			String symbol = stock.getString("symbol").split(" ")[0];
			
			if (col == getColumnIndex("alpha last") ) {
				String alphaQuery = String.format(
						"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
						symbol, 
						AlphaKey);

				MyClient.create(alphaQuery)
					.header("accept", "application/json")
					.header("content-type", "application/json")
					.query( resp -> {  
				  			Util.require( resp.statusCode() == 200, "Error status code %s - %s", 
				  					resp.statusCode(), resp.body() );
				  			S.out( resp.body() );

				  			JsonObject obj = JsonObject.parse(resp.body() );
				  			obj.display();
				  			double last = obj.getObject("Global Quote").getDouble("05. price");
				  			stock.put("alpha last", last);
				  			fireTableRowsUpdated(row, row);
					});
			}
		}
	}
}
