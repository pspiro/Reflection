package coinstore;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import coinstore.Coinstore.Aapl;
import common.JsonModel;
import common.Util;
import http.BaseTransaction;
import http.MyClient;
import http.MyServer;
import tw.util.HtmlButton;
import tw.util.NewLookAndFeel;
import tw.util.S;
import tw.util.VerticalPanel.FlowPanel;

public class CsServer {
	private static final String refPriceUrl = "https://reflection.trading/api/get-price/265598";
	private static final double rndSize = 0;
	private static final double[] sizes = { 1, 2, 3, 10 };
	private static final double[] offsets = { .003, .007, .01, .02 };
	private static final double tolerance = .011; // change in bid/ask smaller than this will be ignored
	private static final String ordPrice = "ordPrice"; 
	private static final String orderQty = "leavesQty";
	private static final String ordSide = "side";
	
	private CsConfig m_config = new CsConfig();
	private JsonModel m_model = new CsModel("side,leavesQty,ordPrice,ordId");
	private JsonObject m_bidRow = new JsonObject();
	private JsonObject m_askRow = new JsonObject();
	private double lastBid = 0;
	private double lastAsk = 0;

	private JsonArray openOrders() { return m_model.ar(); }  // fields are side, leavesQty, ordPrice

	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("CsServer");		
			NewLookAndFeel.register();
			
			new CsServer();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);  // we need this because listening on the port will keep the app alive
		}
	}

	CsServer() throws Exception {
		MyServer.listen( m_config.port(), 10, server -> {
			server.createContext("/csserver/ok", exch -> new CsTransaction(exch, false).respondOk() ); 
			//server.createContext("/csserver/status", exch -> new CsTransaction(this, exch).onStatus() ); 
		});
		
		m_bidRow.put( ordSide, "BID");
		m_askRow.put( ordSide, "ASK");
		
		HtmlButton refresh = new HtmlButton( "Refresh", ev -> onRefresh() );
		HtmlButton check = new HtmlButton( "Check", ev -> Util.wrap( () -> check() ) );
		HtmlButton cancelAll = new HtmlButton( "Cancel all", ev -> onCancelAll() );
		FlowPanel butPanel = new FlowPanel(20, 5, refresh, check, cancelAll);
		
		JFrame f = new JFrame();
		f.add( butPanel, BorderLayout.NORTH);
		f.add( m_model.createTable() );
		f.setSize( 800, 800);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE );
		f.setVisible(true);

		// download orders and display in table
		downloadOrders(MyClient.getJson( refPriceUrl));
		
		//Util.executeEvery(0, m_config.interval(), () -> check() );
		//check();
	}
	
	private void onCancelAll() {
		Util.wrap( () -> {
			openOrders().forEach( order -> Util.wrap( () ->
				aapl.cancel( order.getLong("ordId") ) ) );
		});
		
	}

	private void onRefresh() {
		Util.wrap( () -> downloadOrders(MyClient.getJson( refPriceUrl) ) );
	}
	
	void check() throws Exception {
		S.out( "checking");
		
		JsonObject bidAsk = MyClient.getJson( refPriceUrl);  // change this to get-cs-price or whatever
		double bid = bidAsk.getDouble( "bid");
		double ask = bidAsk.getDouble( "ask");
		
		JsonArray newOrders = new JsonArray();

		// change in price?
		if (!Util.isEq( bid, lastBid, tolerance) || !Util.isEq( ask, lastAsk, tolerance) ) {
			S.out( "Updating orders  bid=%s  ask=%s", bid, ask);
			
			// create new orders
			for (int i = 0; i < sizes.length; i++) {
				newOrders.add( new Rec(
						Action.Buy, 
						randomize( sizes[i], rndSize), 
						bid - bid * offsets[i]) 
				);
				newOrders.add( new Rec( 
						Action.Sell, 
						randomize( sizes[i], rndSize), 
						ask + ask * offsets[i])
				);
			}

			// place new orders and set order ids
//			for (JsonObject order : newOrders) {
//				place(order);
//			}
			
			// cancel existing orders
			openOrders().forEach( order -> Util.wrap( () ->
				aapl.cancel( order.getLong("ordId") ) ) );
			
			// update array
			updateOrders( newOrders);
			updateBidAsk( bidAsk);
		}
		
		// save new values
		lastBid = bid;
		lastAsk = ask;
	}

	static Aapl aapl = new Aapl();
	
	/** @return order id 
	 * @throws Exception */
	void place(JsonObject order) throws Exception {
		
		long id = aapl.placeOrder(
				order.getEnum(ordSide, Action.values()),
				order.getDouble(orderQty),
				order.getDouble(ordPrice) );

		// add order id to order so it can be canceled later
		order.put("ordId", id);
	}

	/** Return i +/- offset of zero to pct */
	private double randomize(double val, double pctIn) {
		if (pctIn == 0) {
			return val;
		}
		double offset = Util.rnd.nextDouble( pctIn) * val;
		return Util.rnd.nextBoolean() ? val + offset : val - offset; 
	}

	class CsTransaction extends BaseTransaction {
		public CsTransaction(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}
	}

	class CsModel extends JsonModel {
		public CsModel(String allNames) {
			super(allNames);
		}
		
		@Override protected Object format(String key, Object value) {
			if (key.equals(ordPrice) || key.equals(orderQty) ) {
				return S.fmt2c( (Double)value);
			}
			return value;
		}
		
		@Override protected void onCtrlClick(JsonObject row, String tag) {
			Util.wrap( () -> {
				aapl.cancel( row.getLong("ordId") );
				onRefresh();
			});
		}
	}
	
	private void downloadOrders(JsonObject bidAsk) throws Exception {
		S.out( "downloading orders");
	
		updateOrders( Coinstore.getOpenOrders() );
		updateBidAsk( bidAsk);
	}
	
	private void updateOrders(JsonArray orders) {
		openOrders().clear();
		openOrders().addAll( orders);
		openOrders().add( m_bidRow);
		openOrders().add( m_askRow);
	}		
	
	private void updateBidAsk(JsonObject bidAsk) {
		m_bidRow.put( ordPrice, bidAsk.getDouble( "bid") );
		m_askRow.put( ordPrice, bidAsk.getDouble( "ask") );

		openOrders().sortJson( ordPrice, false);
		
		m_model.fireTableDataChanged();
	}

	static class Rec extends JsonObject {
		Rec( Action side, double size, double price) {
			put( ordSide, side);
			put( orderQty, size);
			put( ordPrice, price);
		}
		
//		@Override public String toString() {
//			return String.format("%s %s at %s", side, S.fmt(size), S.fmt(price));
//		}
	}
	
}

//bidSpreadAbs
//bidSpreadPct
//askSpread
//totalSpread
//buyQty
//buyAmt
//sellQty
//sellAmt
//
//BUY
//SELL
//TOTAL
//


// TODO
// when the market is closed, you have to be more intelligent; move away as orders are filled
// jsonobject.getdouble, don't convert to string and back if not necessary'