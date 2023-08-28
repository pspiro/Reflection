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
		super( "id,wallet_public_key,stablecoin,amount,fulfilled,created_at,REDEEM NOW",
			   "select * from redemptions");
	}
	
	@Override protected JsonModel createModel(String allNames, String sql) {
		return new RedemptionModel(allNames, sql);
	}
	
	class RedemptionModel extends QueryModel {
		RedemptionModel(String allNames, String sql) {
			super(allNames, sql);
		}
	
		public void onLeftClick(MouseEvent e, int row, int col) {
			if (col == getIndex("REDEEM NOW") ) {
				Util.wrap( () -> {
					redeem(m_ar.get(row));
				});
			}
		}
		
		private void redeem(JsonObject obj) throws Exception {
			if (obj.getBool("fulfilled") ) {
				S.inform(RedemptionPanel.this, "Already filled");
				return;
			}
				
			String walletAddr = obj.getString( "wallet_public_key");
			Rusd rusd = Monitor.m_config.rusd();
			Busd busd = Monitor.m_config.busd();

			double rusdPos = rusd.getPosition(walletAddr);  // make sure that rounded amt is not slightly more or less
			if (rusdPos < .005) {
				S.inform(RedemptionPanel.this, "User has no RUSD to redeem");
				return;
			}
			
			double ourStablePos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (ourStablePos < rusdPos) {
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						walletAddr, rusdPos, ourStablePos, (rusdPos - ourStablePos) );
				S.inform( RedemptionPanel.this, str);
				return;
			}
			
			if (!S.confirm(RedemptionPanel.this, "Are you sure?") ) {
				return;
			}
			
			rusd.sellRusd(walletAddr, busd, rusdPos)
				.waitForHash();
			S.inform( RedemptionPanel.this, "Completed");
		}

		@Override public void adjust(JsonObject obj) {
			obj.update( "created_at", val -> Util.left( val.toString(), 19) );
		}
	}
	
}
