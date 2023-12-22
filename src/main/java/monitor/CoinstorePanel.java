package monitor;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import coinstore.Coinstore;
import common.Util;
import monitor.Monitor.MonPanel;
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
			super(new BorderLayout(), "currency,available,frozen,total");
			m_model.justify("lrrr");  // let numbers be right-aligned
			add( m_model.createTable() );
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

			m_model.m_ar = ar;
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
			super( new BorderLayout(), "matchTime,side,execQty,execAmt,matchRole,orderId,instrumentId,fee,quoteCurrencyId,baseCurrencyId,orderState,acturalFeeRate,feeCurrencyId,id,remainingQty,matchId,tradeId");
			add( m_model.createTable() );
			m_model.justify( "llrr");
			// matchRole, TAKER(1),MAKER(-1) remove ro
		}
		
		/** Load up existing trades */
		@Override public void refresh() throws Exception {
			m_model.m_ar = Coinstore.getTrades(symbol);
			m_model.fireTableDataChanged();
			
			m_model.m_ar.forEach( trade -> ids.add( trade.getString(tag) ) ); // add all id's to set
			
			//Util.executeEvery(period, period, () -> check() );
		}
		
		@Override protected Object format(String key, Object value) {
			switch( key) {
				case "matchTime":
					return Util.yToS.format( Long.valueOf(value.toString()) * 1000);
				case "fee":
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
				Coinstore.getTrades(symbol).forEach( trade -> { // will max out at 100 trades
					if (!ids.contains(trade.getString(tag)) ) {
						
						S.out( "THERE WAS A NEW TRADE: " + trade);
						m_model.m_ar.add(trade);
						Monitor.m_config.sendEmail("peteraspiro@gmail.com", "COINSTORE TRADE", trade.toString(), false);

						ids.add(trade.getString(tag));
						
						Util.wrap( () -> refresh() );
					}
				});
			});
		}
	}
}
