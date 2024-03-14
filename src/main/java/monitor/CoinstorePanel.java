package monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.Box;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import coinstore.Coinstore;
import common.Util;
import tw.util.S;

class CoinstorePanel extends MonPanel {
	PositionsPanel positionsPanel;
	TradesPanel tradesPanel;

	CoinstorePanel() {
		super( new BorderLayout() );
		positionsPanel = new PositionsPanel();
		tradesPanel = new TradesPanel();

		add( positionsPanel, BorderLayout.NORTH);
		add( tradesPanel);
	}

	@Override public void activated() {
		positionsPanel.activated();
		tradesPanel.activated();
	}

	@Override public void refresh() throws Exception {
		positionsPanel.refresh();
		tradesPanel.refresh();
	}

	static class PositionsPanel extends JsonPanel {
		PositionsPanel() {
			super(new FlowLayout(), "currency,available,frozen,total");
			m_model.justify("lrrr");  // let numbers be right-aligned
			add( m_model.createNoScroll() );
		}
		
		@Override public void refresh() throws Exception {
			HashMap<String,JsonObject> map = new HashMap<>();
			Coinstore.getPositions().forEach( pos -> {
				JsonObject record = Util.getOrCreate(map, pos.getString("currency"), () -> new JsonObject() );
				
				record.put("currency", pos.getString("currency"));
				
				double balance = pos.getDouble("balance");
				if (pos.getString("typeName").equalsIgnoreCase("Frozen")) {
					record.put( "frozen", balance);
				}
				else {
					record.put( "available", balance);
				}
				record.increment( "total", balance);
			});
			
			JsonArray ar = new JsonArray();
			map.values().forEach( val -> ar.add(val));

			setRows( ar);
			m_model.fireTableDataChanged();
		}

		@Override protected Object format(String key, Object value) {
			switch( key) {
				case "available":
				case "frozen":
				case "total":
					return S.fmt(value.toString());
			}
			return value;
		}
	}

	static class TradesPanel extends JsonPanel {
		static final String tag = "id"; // there is also "tradeId" which seems to be unique; not sure which to use

		int period = 5000;
		String symbol = "AAPLUSDT";
		HashSet<String> ids = new HashSet<>();
		
		TradesPanel() {
			super( new BorderLayout(), "matchTime,side,execQty,price,execAmt,matchRole,orderId,instrumentId,fee,orderState,acturalFeeRate,feeCurrencyId,id,remainingQty,matchId,tradeId");
			add( Box.createVerticalStrut(20), BorderLayout.NORTH);
			add( m_model.createTable() );
			m_model.justify( "llrrr");
		}
		
		@Override public void activated() {
			refreshTop();
			
			S.out( "Monitoring for new trades");
			Util.executeEvery(period, period, () -> check() );
		}
		
		/** Load up existing trades */
		@Override public void refresh() throws Exception {
			setRows( Coinstore.getAllTrades(symbol) );
			m_model.fireTableDataChanged();
			
			rows().forEach( 
					trade -> ids.add( trade.getString(tag) ) ); // add all id's to set

			rows().forEach( trade -> 
				trade.put( "price", trade.getDouble("execAmt") / trade.getDouble("execQty") ) );
		}
		
		@Override protected Object format(String key, Object value) {
			switch( key) {
				case "matchTime":
					return Util.yToS.format( Long.valueOf(value.toString()) * 1000);
				case "fee":
				case "price":
				case "execQty":
				case "execAmt":
					return S.fmt(value.toString());
				case "side":
					return value.toString().equals("-1") ? "Sell" : value.toString().equals("1") ? "Buy" : value;
				case "matchRole":
					return ((Long)value) == 1 ? "Taker" : "Maker";
			}
			return value;
		}

		/** Check for new trades */
		private void check() {
			Util.wrap( () -> {
				Coinstore.getLatestTrades(symbol).forEach( trade -> { // will max out at 100 trades
					if (!ids.contains(trade.getString(tag)) ) {
						
						S.out( "THERE WAS A NEW TRADE: " + trade);
						rows().add(trade);
						Monitor.m_config.sendEmail("peteraspiro@gmail.com", "COINSTORE TRADE", trade.toString());

						ids.add(trade.getString(tag));
					}
				});
			});
		}
	}
}
