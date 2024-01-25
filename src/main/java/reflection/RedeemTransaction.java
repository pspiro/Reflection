package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Rusd;
import tw.util.S;
import util.LogType;

public class RedeemTransaction extends MyTransaction implements LiveTransaction {
	enum LiveStatus {
		Working, Delayed, Completed, Failed
	}

	private LiveStatus m_status = LiveStatus.Working;   // move up to base class
	private int m_progress = 5;  // only relevant if status is working; range is 0-100
	private String m_text;  // text returned to Frontend

	RedeemTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
	}
	
	/** Return database table name */
	@Override public String tableName() {
		return "redemptions";
	}

	/** Redeem (sell) RUSD */ 
	public void handleRedeem() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
						
			require( m_config.allowRedemptions(), RefCode.REDEMPTIONS_HALTED, "Redemptions are temporarily halted. Please try again in a little while.");
			require( m_main.validWallet( m_walletAddr, Action.Sell), RefCode.ACCESS_DENIED, "Your redemption cannot be processed at this time (L6)");  // make sure wallet is not blacklisted

			// cookie comes in the message payload (could easily be changed to Cookie header, just update validateCookie() ) 
			parseMsg();
			validateCookie("redeem");
			
			Rusd rusd = m_config.rusd();
			Busd busd = m_config.busd();

			// note there is some delay after a completed transaction before it is reflected 
			// in the wallet position that would cause this test to pass or fail erroneously
			double rusdPos = Util.truncate( rusd.getPosition(m_walletAddr), 4); // truncate after four digits because Erc20 rounds to four digits when converting to Blockchain mode
			require( rusdPos > .004, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			// check for previous unfilled request (either Delayed or Submitted) 
			require( Main.m_config.sqlQuery( conn -> conn.queryToJson( "select * from redemptions where wallet_public_key = '%s' and (status = 'Delayed' or status = 'Working')", m_walletAddr.toLowerCase()) ).isEmpty(), 
					RefCode.REDEMPTION_PENDING, 
					"There is already an outstanding redemption request for this wallet; we appreciate your patience");
	
			double busdPos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= rusdPos) {  // we don't have to worry about decimals here, it shouldn't come down to the last penny
				olog( LogType.REDEEM, "amount", rusdPos);

				String fbId = rusd.sellRusd(m_walletAddr, busd, rusdPos).id();  // rounds to 4 decimals, but RUSD can take 6; this should fail if user has 1.00009 which would get rounded up
				
				respond( code, RefCode.OK, "id", m_uid);  // we return the uid here to be consisten with the live order processing, but it's not really needed since Frontend can only have one Redemption request open at a time

				insertRedemption( busd, rusdPos, fbId); // informational only, don't throw an exception
				
				// this redemption will now be tracked by the live order system
				liveRedemptions.put( m_walletAddr.toLowerCase(), this);
				allLiveTransactions.put( fbId, this);
			}
			else {
				// write unfilled report to DB
				m_status = LiveStatus.Delayed; // stays in this state until the redemption is manually sent by operator
				insertRedemption( busd, rusdPos, null);
				
				// send alert email so we can move funds from brokerage to wallet
				String str = String.format( 
						"Insufficient stablecoin in RefWallet for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						m_walletAddr, rusdPos, busdPos, (rusdPos - busdPos) );
				alert( "MOVE FUNDS NOW TO REDEEM RUSD", str);
				
				// report error back to user
				throw new RefException( RefCode.INSUFFICIENT_FUNDS, "Your redemption request is being processed; we appreciate your patience");
			}
		});
	}

	
	@Override public synchronized void onUpdateFbStatus(FireblocksStatus status, String hash) {
		if (m_status == LiveStatus.Working) {
			m_progress = status.pct();
	
			if (status == FireblocksStatus.COMPLETED) {
				m_status = LiveStatus.Completed;
				m_text = "The redemption was successful";  // will be returned to Frontend in api/mywallet msg
				
				// update db
				updateRedemption( LiveStatus.Completed);

				// log
				jlog( LogType.REDEMPTION_COMPLETED, null);
			}
			else if (status.pct() == 100) {
				m_status = LiveStatus.Failed;
				m_text = "The redemption failed";  // will be returned to Frontend in api/mywallet msg
				
				// update db
				updateRedemption( LiveStatus.Failed);
			
				// log
				olog( LogType.REDEMPTION_FAILED, Message, "The blockchain transaction failed with status " + status);
			}
		}
	}
	
	/** no exceptions */
	private void insertRedemption(Busd busd, double rusdPos, String fbId) {
		Util.wrap( () -> {
			JsonObject obj = new JsonObject();
			obj.put( "uid", m_uid);
			obj.put( "wallet_public_key", m_walletAddr.toLowerCase() );
			obj.put( "stablecoin", busd.name() );
			obj.put( "amount", rusdPos);
			obj.put( "status", m_status);
			
			if (S.isNotNull(fbId) ) {
				obj.put( "fireblocks_id", fbId);
			}
	
			m_main.queueSql( conn -> conn.insertJson("redemptions", obj) );
		});
	}

	private void updateRedemption(LiveStatus status) {
		m_main.queueSql( sql -> sql.execWithParams( 
				"update redemptions set status = '%s' where uid = '%s'", status, m_uid) );
		
		if (!m_map.getBool("testcase")) {
			alert( "REDEMPTION", status.toString() );
		}
	}

	public synchronized int progress() {
		return m_progress;
	}

	public String text() {
		return m_text;
	}

	public LiveStatus status() {
		return m_status;
	}
}

// now you just have to return the redemption info in the MyWallet query
