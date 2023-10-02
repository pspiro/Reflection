package monitor;


import java.awt.event.MouseEvent;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Rusd;
import tw.util.S;

public class RedemptionPanel extends QueryPanel {

	RedemptionPanel() {
		super( "created_at,id,wallet_public_key,stablecoin,amount,fulfilled,REDEEM NOW",
			   "select * from redemptions order by created_at desc $limit");
	}
	
	@Override protected JsonModel createModel(String allNames, String sql) {
		return new RedemptionModel(allNames, sql);
	}
	
	@Override public void adjust(JsonObject obj) {
		obj.update( "created_at", val -> Util.left( val.toString(), 19) );
	}

	class RedemptionModel extends QueryModel {
		RedemptionModel(String allNames, String sql) {
			super(allNames, sql);
		}
	
		public void onLeftClick(MouseEvent e, int row, int col) {
			if (col == getColumnIndex("REDEEM NOW") ) {
				Util.wrap( () -> {
					redeem(m_ar.get(row));
				});
			}
		}
		
		private void redeem(JsonObject redemption) throws Exception {
			String walletAddr = redemption.getString( "wallet_public_key");
			Rusd rusd = Monitor.m_config.rusd();
			Busd busd = Monitor.m_config.busd();

			// already fulfilled?
			if (redemption.getBool("fulfilled") ) {
				S.inform(RedemptionPanel.this, "Already filled");
				return;
			}
		
			// nothing to redeem?
			double rusdPos = rusd.getPosition(walletAddr);  // make sure that rounded amt is not slightly more or less
			if (rusdPos < .005) {
				S.inform(RedemptionPanel.this, "User has no RUSD to redeem");
				return;
			}
			
			// insufficient stablecoin in RefWallet?
			double ourStablePos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (ourStablePos < rusdPos) {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						walletAddr, rusdPos, ourStablePos, (rusdPos - ourStablePos) );
				S.inform( RedemptionPanel.this, str);
				return;
			}
			
			if (S.confirm(RedemptionPanel.this, "Are you sure?") ) {
				rusd.sellRusd(walletAddr, busd, rusdPos)
					.waitForHash();
				
				// update redemptions table in DB and screen
				String sql = String.format( "update redemptions set fulfilled=true where id = %s", redemption.getInt("id") );
				Monitor.m_config.sqlCommand( conn -> conn.execute(sql) );
				
				refresh();
				
				S.inform( RedemptionPanel.this, "Completed");
			}
		}

	}
	
}
