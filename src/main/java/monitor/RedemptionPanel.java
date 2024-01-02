package monitor;


import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Rusd;
import fireblocks.Transactions;
import tw.util.S;

public class RedemptionPanel extends QueryPanel {

	RedemptionPanel() {
		super(	"redemptions",
				"created_at,uid,fireblocks_id,wallet_public_key,blockchain_hash,status,stablecoin,amount,REDEEM NOW",
				"select * from redemptions order by created_at desc $limit");
	}
	
	@Override protected JsonModel createModel(String allNames) {
		return new Model(allNames);
	}
	
	@Override public void adjust(JsonObject obj) {
		obj.update( "created_at", val -> Util.left( val.toString(), 19) );
	}
	
	@Override protected Object format(String key, Object value) {
		return S.notNull(key).equals("REDEEM NOW") ? "R" : super.format(key, value);
	}
	
	@Override protected void onDouble(String tag, Object val) {
		switch(tag) {
		case "uid":
			Monitor.m_tabs.select( "Log");
			Monitor.m_logPanel.filterByUid(val.toString());
			break;
		case "fireblocks_id":
			Util.wrap( () -> Transactions.getTransaction(val.toString()).display() );
			break;
		case "blockchain_hash":
			// show in explorer
			break;
			default:
				super.onDouble(tag, val);
		}
	}

	class Model extends QueryModel {
		Model(String allNames) {
			super(allNames);
		}
	
		public void onLeftClick(MouseEvent e, int row, int col) {
			if (col == getColumnIndex("REDEEM NOW") ) {
				Util.wrap( () -> {
					redeem(m_ar.get(row));
				});
			}
		}
		
		static DecimalFormat six = new DecimalFormat("#,###.000000");
		
		private void redeem(JsonObject redemption) throws Exception {
			String walletAddr = redemption.getString( "wallet_public_key");
			Rusd rusd = Monitor.m_config.rusd();
			Busd busd = Monitor.m_config.busd();
			
			// already fulfilled?
			if (!redemption.getString("status").equals("Delayed") ) {
				Util.inform(RedemptionPanel.this, "Only 'Delayed' status can be redeemed");
				return;
			}
		
			// nothing to redeem?
			double rusdPos = rusd.getPosition(walletAddr);  // make sure that rounded amt is not slightly more or less
			if (rusdPos < .005) {
				Util.inform(RedemptionPanel.this, "User has no RUSD to redeem");
				return;
			}
			
			if (!Util.confirm(
					RedemptionPanel.this, 
					String.format("Are you sure you want to redeem %s RUSD for %s?",
							six.format(rusdPos), walletAddr) ) ) {
				return;
			}
			
			// insufficient stablecoin in RefWallet?
			double ourStablePos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (ourStablePos < rusdPos) {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						walletAddr, rusdPos, ourStablePos, (rusdPos - ourStablePos) );
				Util.inform( RedemptionPanel.this, str);
				return;
			}
			
			// dont tie up the UI thread
			Util.executeAndWrap( () -> {
				String hash = rusd.sellRusd(walletAddr, busd, rusdPos)
					.waitForHash();

				// update redemptions table in DB and screen
				String sql = String.format( 
						"update redemptions set status = 'Completed', blockchain_hash = '%s' where uid = '%s'", 
						hash, redemption.getString("uid") );
				Monitor.m_config.sqlCommand( conn -> conn.execute(sql) );
				
				RedemptionPanel.this.refresh();
				Util.inform( RedemptionPanel.this, "Completed");
			});
		}
	}
	
}
