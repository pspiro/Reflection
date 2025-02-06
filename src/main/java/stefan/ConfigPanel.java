package stefan;

import java.awt.Component;

import javax.swing.JLabel;

import common.Util;
import tw.util.UI;
import tw.util.UpperField;
import tw.util.VerticalPanel;

public class ConfigPanel extends VerticalPanel {
	private UpperField host = new UpperField( 14);
	private UpperField port = new UpperField();
	private UpperField clientId = new UpperField();
	private UpperField stockList = new UpperField( 21);
	private UpperField startTime = new UpperField();
	private UpperField endTime = new UpperField();
	private UpperField closeTime = new UpperField();
	private UpperField candleSize = new UpperField();
	private UpperField totalAmount = new UpperField().right();
	private UpperField tradeAmount = new UpperField().right();
	private UpperField minShares = new UpperField().right();
	private TCombo<String> orderType = new TCombo<String>("MKT", "LMT");
	private UpperField limitOffset = new UpperField(); // in USD
	private UpperField trailingPct = new UpperField(); // store and transmitted as pct, not decimal
	private UpperField lossLimitPct = new UpperField();
	private UpperField excessPct = new UpperField();

	private Stefan m_stefan;


	public ConfigPanel(Stefan stefan) {
		m_stefan = stefan;
		
		addHeader("Connection Parameters");
		add( "Host / IP address", host);
		add( "Port", port);
		add( "Client Id", clientId);

		addHeader("Stock List");
		add( "Stock symbols (comma-separated)", stockList);

		addHeader( "Timing and Candles");
		add( "Candle phase start time", startTime, lab( "EST (24 hour)"));
		add( "Candle phase end time", endTime, lab( "EST"));
		add( "Trading session end time", closeTime, lab( "EST"));
		add( "Candle size", candleSize, lab( "minutes") );

		addHeader( "Trade Sizes");
		add( "Total amount", totalAmount, lab( "USD") );
		add( "Trade amount", tradeAmount, lab( "USD") );
		add( "Min shares per order", minShares, lab( "shares") );

		addHeader( "Buy Order Settings");
		add( "Price buffer", excessPct, lab( "%  ('last' price must exceed buyLine by this amount)") );
		add( "Order type", orderType);
		add( "Limit price offset", limitOffset, lab( "USD  (applies only if LMT is selected)"));

		addHeader( "Sell Order Settings");
		add( "Trailing percent", trailingPct, lab( "%"));
		add( "Loss limitation offset", lossLimitPct, lab( "%"));
		
		addHeader( "Save");
		add( "", UI.button( "Save", ev -> save() ), UI.button( "Revert", ev -> revert() ) );
	}
	
	private static Component lab(String string) {
		return new JLabel( string);
	}

	void refresh(Params params) {
		host.setString( params.host() );
		port.setString( params.port() );
		clientId.setString( params.clientId() );
		stockList.setString( params.stockList() );
		startTime.setString( params.startTime() );
		endTime.setString( params.endTime() );
		closeTime.setString( params.closeTime() );
		candleSize.setString( params.candleSize() );
		totalAmount.set2d( params.totalAmount() );
		tradeAmount.set2d( params.tradeAmount() );
		minShares.setString( params.minShares() );
		orderType.setSelectedItem( params.orderType() );
		limitOffset.set2d( params.limitOffset() );
		trailingPct.set2d( params.trailingPct() );
		lossLimitPct.setPercent( params.lossLimitPct() );
		excessPct.setPercent( params.excessPct() );  // aka priceBuffer
	}
		
	void save() {
		var params = new Params(
				host.getString(),		
				port.getInt(),			
				clientId.getInt(),		
				stockList.getString(),	
				startTime.getString(),	
				endTime.getString(),		
				closeTime.getString(),	
				candleSize.getInt(),	
				totalAmount.getDouble(),	
				tradeAmount.getDouble(),	
				minShares.getInt(),		
				orderType.getSelectedItem(),
				limitOffset.getDouble(),	
				trailingPct.getDouble(),	
				lossLimitPct.getPercent(),	
				excessPct.getPercent()	  // aka priceBuffer
				);
		
		try {
			params.validate();
		} catch (Exception e) {
			//Util.inform( this, "Fix errors before saving");
			Util.inform( this, e.getMessage() );
			return;
		}
		
		try {
			params.write();
			m_stefan.updateParams( params);
			UI.flash( "Saved");
		} catch (Exception e) {
			e.printStackTrace();
			Util.inform( this, e.getMessage() );
		}

	}
	
	private void revert() {
		if (m_stefan.params() != null) {
			refresh( m_stefan.params() );
		}
	}

}
