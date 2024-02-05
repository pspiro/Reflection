package monitor;


import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Rusd;
import fireblocks.Transactions;

public class RedemptionPanel extends QueryPanel {
	static DecimalFormat six = new DecimalFormat("#,###.000000");

	RedemptionPanel() {
		super(	"redemptions",
				"created_at,uid,fireblocks_id,wallet_public_key,blockchain_hash,status,stablecoin,amount",
				"select * from redemptions $where order by created_at desc $limit");
	}

	@Override public void adjust(JsonObject obj) {
		obj.update( "created_at", val -> Util.left( val.toString(), 19) );
	}

	@Override protected void onRightClick(MouseEvent e, JsonObject record, String tag, Object val) {
		JPopupMenu m = new JPopupMenu();
		m.add( JsonModel.menuItem("Copy", ev -> Util.copyToClipboard(val) ) );
		m.add( JsonModel.menuItem("Redeem", ev -> redeem( record) ) );
		m.show( e.getComponent(), e.getX(), e.getY() );
	}

	@Override protected void onDouble(String tag, Object val) {
		switch(tag) {
		case "wallet_public_key":
			Monitor.m_tabs.select( "Wallet");
			Monitor.m_walletPanel.setWallet( val.toString() );
			break;
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

	private void redeem(JsonObject redemption) {
		Util.wrap( () -> redeem_(redemption) );
	}

	private void redeem_(JsonObject redemption) throws Exception {
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

		// insufficient stablecoin in RefWallet?
		double ourStablePos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
		if (ourStablePos < rusdPos) {
			String str = String.format( 
					"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
					walletAddr, rusdPos, ourStablePos, (rusdPos - ourStablePos) );
			Util.inform( RedemptionPanel.this, str);
			return;
		}

		// confirm
		if (!Util.confirm(
				RedemptionPanel.this, 
				String.format("Are you sure you want to redeem %s RUSD for %s?",
						six.format(rusdPos), walletAddr) ) ) {
			return;
		}

		// dont tie up the UI thread
		Util.executeAndWrap( () -> {
			String hash = rusd.sellRusd(walletAddr, busd, rusdPos)
			
			// already fulfilled?

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
