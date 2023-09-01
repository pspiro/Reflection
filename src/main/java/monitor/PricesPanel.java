package monitor;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import common.Util;
import monitor.Monitor.RefPanel;
import tw.util.S;

public class PricesPanel extends JPanel implements RefPanel {
	final Model m_model = new Model();
	final static String AlphaKey = "EKD9A4ZUSQPEPFXK";  // alphavantage API key
	final static String AlphaUrl = "https://www.alphavantage.co";
	
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
			super( "symbol,conid,bid,ask,last,time,alpha last,alpha bid,alpha ask");
		}
		
		/** Called at startup */
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
			Monitor.queryObj("/api/?msg=getallprices", map -> {
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

				AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
				client
					.prepare("GET", alphaQuery)
					.setHeader("accept", "application/json")
					.setHeader("content-type", "application/json")
				  	.execute()
				  	.toCompletableFuture()
				  	.thenAccept( response -> {
				  		try {
				  			client.close();
				  			
				  			Util.require( response.getStatusCode() == 200, "Error status code %s - %s", 
				  					response.getStatusCode(), response.getStatusText() );
				  			
				  			S.out( response.getResponseBody() );
				  			JsonObject obj = JsonObject.parse(response.getResponseBody());
				  			obj.display();
				  			double last = obj.getObject("Global Quote").getDouble("05. price");
				  			stock.put("alpha last", last);
				  			fireTableRowsUpdated(row, row);
				  		}
				  		catch (Exception e) {
				  			e.printStackTrace();
				  		}
				  	});
			}
		}
	}
	
	
	@Override public void activated() {
		m_model.refresh();
	}

	@Override public void closed() {
	}

}
