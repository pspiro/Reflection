package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import http.MyClient;
import tw.util.S;

/** RefAPI prices panel. This panel reads the spreadsheet directly */
public class PricesPanel extends JsonPanel {
	final static String AlphaKey = "EKD9A4ZUSQPEPFXK";  // alphavantage API key
	final static String AlphaUrl = "https://www.alphavantage.co";

	PricesPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,time");
		
		JPanel southPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 50, 20) );
		southPanel.add( new JLabel( "NOTE: you would have to reload the Monitor to see changes to the spreadsheet reflected here") );
		
		add( m_model.createTable() );
		add( southPanel, BorderLayout.SOUTH);
		m_model.justify("llrrr");
	}
	
	@Override JsonModel createModel(String allNames) {
		return new Model(allNames);
	}

	@Override public void activated() {
		S.out( "Initializing Prices panel");

		Monitor.stocks().forEach( stockIn -> {
			JsonObject stock = new JsonObject();
			stock.put( "symbol", stockIn.symbol() );
			stock.put( "conid", stockIn.rec().conid() );
			rows().add( stock);
		});

		m_model.fireTableDataChanged();

		refreshTop();
	}

	@Override protected void refresh() throws Exception {
		MyClient.getJson(Monitor.refApiBaseUrl() + "/api/?msg=getallprices", json -> {
			json.forEach( (conid,prices) -> update(Integer.parseInt(conid), (JsonObject)prices) ); 
			m_model.fireTableDataChanged();
		});
	}

	@Override protected Object format(String key, Object value) {
		return key.equals("bid") || key.equals("ask") || key.equals("last")
				? MdsPricesPanel.fmtPrice(value) 
				: value;
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
		for (JsonObject stock : rows() ) {
			if (stock.getInt("conid") == conid) {
				return stock;
			}
		}
		return null;
	}

	class Model extends JsonPanelModel {
		public Model(String allNames) {
			super(allNames);
		}

		@Override public void onLeftClick(MouseEvent event, int row, int col) {
			JsonObject stock = m_model.getRow(row);
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
					m_model.fireTableRowsUpdated(row, row);
				});
			}
		}
	}
}
