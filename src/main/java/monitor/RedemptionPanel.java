package monitor;


import java.text.DecimalFormat;

import javax.swing.JPopupMenu;

import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import tw.util.UI;
import web3.Busd;
import web3.Rusd;

public class RedemptionPanel extends QueryPanel {
	static DecimalFormat six = new DecimalFormat("#,###.000000");

	RedemptionPanel() {
		super(	"redemptions",
				"created_at,uid,wallet_public_key,first_name,last_name,status,amount",
				"""
select redemptions.created_at,redemptions.uid,redemptions.wallet_public_key,first_name,last_name,status,amount
from redemptions
left join users using (wallet_public_key)
$where
order by created_at desc
$limit""");
	}
	
	@Override public void adjust(JsonObject obj) {
		obj.update( "created_at", val -> Util.left( val.toString(), 19) );
	}

	@Override protected void buildMenu(JPopupMenu m, JsonObject record, String tag, Object val) {
		m.add( JsonModel.menuItem("Redeem", ev -> redeem( record) ) );
		m.add( JsonModel.menuItem("Delete", ev -> delete( record) ) );
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
		case "blockchain_hash":
			Util.browse( config().blockchainTx( val.toString() ) );
			break;
		default:
			super.onDouble(tag, val);
		}
	}

	void delete(JsonObject rec) {
		// confirm
		if (Util.confirm( RedemptionPanel.this, "Are you sure you want to delete this record?") ) {
			try {
				Monitor.m_config.sqlCommand( sql -> sql.delete( "delete from redemptions where uid = '%s'",
					rec.getString("uid") ) );
				UI.flash( "Record deleted");
				refresh();
			}
			catch( Exception e) {
				e.printStackTrace();
				Util.inform( this, "Error - " + e.getMessage() );
			}
		}
	}

	private void redeem(JsonObject redemption) {
		wrap( () -> redeem_(redemption) );
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
		double ourStablePos = busd.getPosition( Monitor.m_config.refWalletAddr() );
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

		// don't tie up the UI thread
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
