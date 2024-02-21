package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonArray;
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
	private double m_quantity; // quantity of RUSD to redeem
	private String m_email;

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
			
			Rusd rusd = m_config.rusd();
			Busd busd = m_config.busd();

			// NOTE: this next code is the same as OrderTransaction

			// make sure user is signed in with SIWE and session is not expired
			// must come before profile and KYC checks
			validateCookie("redeem");
			
			// get record from Users table
			JsonArray ar = Main.m_config.sqlQuery( conn -> conn.queryToJson("select * from users where wallet_public_key = '%s'", m_walletAddr.toLowerCase() ) );  // note that this returns a map with all the null values
			require( ar.size() == 1, RefCode.INVALID_USER_PROFILE, "Please update your profile and then resubmit your request");

			// validate user profile fields
			JsonObject userRecord = ar.get(0);
			Profile profile = new Profile(userRecord);
			profile.validate();
			
			// save email to send alerts later
			m_email = profile.email(); 

			// note there is some delay after a completed transaction before it is reflected 
			// in the wallet position that would cause this test to pass or fail erroneously
			m_quantity = Util.truncate( rusd.getPosition(m_walletAddr), 4); // truncate after four digits because Erc20 rounds to four digits when converting to Blockchain mode
			require( m_quantity > .004, RefCode.INSUFFICIENT_FUNDS, "No RUSD in user wallet to redeem");
			
			// check for previous unfilled request (either Delayed or Submitted) 
			require( Main.m_config.sqlQuery( conn -> conn.queryToJson( "select * from redemptions where wallet_public_key = '%s' and (status = 'Delayed' or status = 'Working')", m_walletAddr.toLowerCase()) ).isEmpty(), 
					RefCode.REDEMPTION_PENDING, 
					"There is already an outstanding redemption request for this wallet; we appreciate your patience");
	
			double busdPos = busd.getPosition( Accounts.instance.getAddress("RefWallet") );
			if (busdPos >= m_quantity && m_quantity <= Main.m_config.maxAutoRedeem() ) {  // we don't have to worry about decimals here, it shouldn't come down to the last penny
				olog( LogType.REDEEM, "amount", m_quantity);

				String fbId = rusd.sellRusd(m_walletAddr, busd, m_quantity).id();  // rounds to 4 decimals, but RUSD can take 6; this should fail if user has 1.00009 which would get rounded up

				insertRedemption( busd, m_quantity, fbId); // informational only, don't throw an exception

				respond( code, RefCode.OK, "id", m_uid);  // we return the uid here to be consisten with the live order processing, but it's not really needed since Frontend can only have one Redemption request open at a time
				
				// this redemption will now be tracked by the live order system
				liveRedemptions.put( m_walletAddr.toLowerCase(), this);
				allLiveTransactions.put( fbId, this);
			}
			else {
				// write unfilled report to DB
				m_status = LiveStatus.Delayed; // stays in this state until the redemption is manually sent by operator
				insertRedemption( busd, m_quantity, null);
				
				// send alert email so we can move funds from brokerage to wallet
				String str = String.format( 
						"Insufficient stablecoin in RefWallet or maxAutoRedeem amount exceeded for RUSD redemption  \nwallet=%s  requested=%s  have=%s  need=%s",
						m_walletAddr, m_quantity, busdPos, (m_quantity - busdPos) );
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
				// set status and text which will be returned the next time the Frontend
				// queries for the liver order statuses
				m_status = LiveStatus.Completed;
				m_text = "The redemption was successful";  // will be returned to Frontend in api/mywallet msg
				
				// update db
				updateRedemption( LiveStatus.Completed);

				// log
				jlog( LogType.REDEMPTION_COMPLETED, null);
				
				// send alert and email (production only)
				if (m_config.isProduction() ) { //&& !m_map.getBool("testcase")) {
					Util.wrap( () -> { 
						alert( "REDEMPTION COMPLETED", S.format( "Converted %s %s to %s for %s", 
								m_quantity, m_config.rusd().name(), m_config.busd().name(), m_walletAddr) );
							
						// send email to the user
						if (Util.isValidEmail( m_email) ) {
							String html = String.format(
									redeemHtml,
									m_quantity,
									m_config.busd().name(),
									m_config.busd().address(),
									m_config.blockchainTx(hash) );
							m_config.sendEmail( m_email, "RUSD has been redeemed on Reflection", html, true);
						}
					});
				}
			}
			else if (status.pct() == 100) {
				m_status = LiveStatus.Failed;
				m_text = "The redemption failed";  // will be returned to Frontend in api/mywallet msg
				
				// update db
				updateRedemption( LiveStatus.Failed);
			
				// log
				olog( LogType.REDEMPTION_FAILED, Message, "The blockchain transaction failed with status " + status);

				Util.wrap( () -> alert( 
						"REDEMPTION FAILED", S.format( "Could not convert %s %s to %s for %s", 
								m_quantity, m_config.rusd().name(), m_config.busd().name(), m_walletAddr) ) );
			}
		}
	}
			
	static final String redeemHtml = """
		<html>
		Your RUSD has been converted to %s %s.<p>
		<p>
		To view the BUSD in your wallet, import this address:<p>
		%s<p>
		You can <a href="%s">view the transaction on the blockchain explorer</a><p>
		</html>
		""";
	
	/** no exceptions, no delay */
	private void insertRedemption(Busd busd, double rusdPos, String fbId) {
		Util.wrap( () -> {
			S.out( "inserting record into redemption table with status %s", m_status);

			JsonObject obj = new JsonObject();
			obj.put( "uid", m_uid);
			obj.put( "wallet_public_key", m_walletAddr.toLowerCase() );
			obj.put( "stablecoin", busd.name() );
			obj.put( "amount", rusdPos);
			obj.put( "status", m_status);
			
			if (S.isNotNull(fbId) ) {
				obj.put( "fireblocks_id", fbId);
			}
	
			m_config.sqlCommand( conn -> conn.insertJson("redemptions", obj) );
		});
	}

	private void updateRedemption(LiveStatus newStatus) {
		Util.wrap( () -> {
			m_config.sqlCommand( sql -> sql.execWithParams( 
					"update redemptions set status = '%s' where uid = '%s'", newStatus, m_uid) );
			
			if (!m_map.getBool("testcase")) {
				alert( "REDEMPTION", newStatus.toString() );
			}
		});
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
	
	/** debug only */
	@Override public String toString() {
		return S.format( "status=%s  progress=%s  text=%s",
				m_status, m_progress, m_text);
	}
}

// now you just have to return the redemption info in the MyWallet query
