package monitor;

import java.awt.BorderLayout;
import java.util.HashSet;

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
			super(new BorderLayout(), "uid,accountId,currency,balance,typeName");
			add( m_model.createTable() );
		}
		
		@Override protected Object format(String key, Object value) {
			switch( key) {
				case "balance":
					return S.fmt(value.toString());
			}
			return value;
		}

		@Override public void refresh() throws Exception {
			m_model.m_ar = Coinstore.getPositions();
			m_model.m_ar.filter( obj -> 
			obj.getString("typeName").equalsIgnoreCase("available") || obj.getDouble("balance") > 0);

			m_model.fireTableDataChanged();
		}
	}

	static class TradesPanel extends JsonPanel {
		static final String tag = "id"; // there is also "tradeId" which seems to be unique; not sure which to use

		int period = 5000;
		String symbol = "USDCUSDT";
		HashSet<String> ids = new HashSet<>();
		
		TradesPanel() {
			super( new BorderLayout(), "side,matchRole,role,orderId,instrumentId,fee,quoteCurrencyId,baseCurrencyId,matchTime,orderState,execAmt,selfDealingQty,accountId,taxRate,acturalFeeRate,feeCurrencyId,id,remainingQty,execQty,matchId,tradeId");
			add( m_model.createTable() );
		}
		
		@Override protected Object format(String key, Object value) {
			switch( key) {
				case "matchTime":
					return Util.hhmmss.format( Long.valueOf(value.toString()) * 1000);
				case "fee":
				case "execAmt":
					return S.fmt(value.toString());
				case "side":
					return value.toString().equals("-1") ? "Sell" : value.toString().equals("1") ? "Buy" : value;
			}
			return value;
		}

		/** Load up existing trades */
		@Override public void activated() {
			Util.wrap( () -> {
				m_model.m_ar = Coinstore.getTrades(symbol);
				m_model.fireTableDataChanged();
				
				m_model.m_ar.forEach( trade -> ids.add( trade.getString(tag) ) ); // add all id's to set
				
				Util.executeEvery(period, period, () -> check() );
			});
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
					}
				});

				m_model.fireTableDataChanged();
			});
		}
	}
}
