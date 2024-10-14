package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

import common.Util;
import reflection.MySqlConnection.MySqlDate;
import tw.util.S;
import util.LogType;
import web3.Busd;
import web3.Rusd;

public class RedeemTransaction extends MyTransaction implements LiveTransaction {
	enum LiveStatus {
		Working,  // redemption was submitted and is being processed on the blockchain 
		Delayed,  // redemption is delayed due to lack of funds in the wallet
		Locked,   // RUSD is locked, seems never used
		Completed, 
		Failed
	}

	private static String WorkingStatuses = "'Working','Delayed','Locked'"; // used for SQL query; you can only have one of these at a time per wallet
	
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
			
			m_quantity = m_map.getDoubleParam( "quantity");
			
			// confirm they have RUSD to redeem; note there is some delay after a completed transaction before it is reflected here
			double pos = rusd.getPosition(m_walletAddr);
			require( pos > .004, RefCode.NO_RUSD_TO_REDEEM, "No RUSD in user wallet to redeem");

			// if no quantity specified, if > pos, or w/in .03 of pos, set it to pos
			if (m_quantity == 0 || m_quantity > pos - .03) {
				m_quantity = Util.truncate( pos, 4); // some parts only handle 4 dec, should be changed to 6... 
			}
			
			m_quantity = Util.truncate( m_quantity, 4); // truncate after four digits because Erc20 rounds to four digits when converting to Blockchain mode

			// don't let them enter very small amounts
			if (pos >= 5) {
				require( m_quantity >= 5, RefCode.INVALID_REQUEST, "Please redeem at least $5.00");
			}
			else {
				require( m_quantity >= pos, RefCode.INVALID_REQUEST, "Please redeem the full amount");
			}
			
			// confirm no Working redemptions
			require( Main.m_config.sqlQuery( 
					"select status from redemptions where wallet_public_key = '%s' and status = 'Working'", 
					m_walletAddr.toLowerCase() ).isEmpty(),
					RefCode.REDEMPTION_PENDING, 
					"There is already an outstanding redemption request for this wallet; we appreciate your patience");
			
			// check if RUSD is locked, meaning they were awarded RUSD and need to wait until 
			// a certain date until redeeming
			String msg = null; // null msg will not get passed to Frontend 
			JsonObject locked = userRecord.getObject( "locked");  // locked fields are: amount, lockedUntil, requiredTrades

			if (locked != null) {
				double remainingDays = Math.max(0., (locked.getLong( "lockedUntil") - System.currentTimeMillis() ) / (double)Util.DAY);
				int remainingTrades = Math.max(0, locked.getInt( "requiredTrades") - numTrades() );  // NOTE numTrades() takes a long time to execute even in dev

				// still locked?
				if (remainingDays > 0 || remainingTrades > 0) {
					// decrement the quantity to redeem by the amount locked
					m_quantity -= locked.getDouble( "amount");
	
					// nothing left?
					require( m_quantity > .001,
							RefCode.RUSD_LOCKED,
							"You must complete %s more trades and/or wait %s more days before redeeming your %s",
							remainingTrades,
							remainingDays,
							Main.m_config.rusd().name() );
					
					msg = "RUSD was partially redeemed; some was locked";
					olog( LogType.PARTIAL_LOCK, locked); 
				}
			}

			// insufficient BUSD in RefWallet or > maxAutoRedeem?
			double busdPos = busd.getPosition( m_config.refWalletAddr() );  // sends query
			double allowance = Main.m_config.getApprovedAmt(); // sends query
			if (m_quantity > busdPos || m_quantity > Main.m_config.maxAutoRedeem() || allowance < m_quantity) {
				// write unfilled report to DB
				insertRedemption( busd, m_quantity, null, LiveStatus.Delayed);  // stays in this state until the redemption is manually sent by operator
				
				// send alert email so we can move funds from brokerage to wallet
				String str = String.format( 
						"Insufficient stablecoin in RefWallet OR maxAutoRedeem amount exceeded for RUSD redemption OR insufficient allowance\n"
						+ "wallet=%s  requested=%s  have=%s  need=%s  maxAuto=%s  allowance=%s",
						m_walletAddr, m_quantity, busdPos, (m_quantity - busdPos), Main.m_config.maxAutoRedeem(), allowance);
				alert( "USER SUBMITTED RUSD REDEMPTION REQUEST", str);
				
				// report error back to user
				throw new RefException( RefCode.DELAYED_REDEMPTION, "Your redemption request is being processed; we appreciate your patience");
			}
			
			// set working status to prevent another redeem attempt
			insertRedemption( busd, m_quantity, "", LiveStatus.Working); // informational only, don't throw an exception

			// redeem it  try/catch here?
			try {
				String hash = rusd.sellRusd(m_walletAddr, busd, m_quantity).waitForHash(); // rounds to 4 decimals, but RUSD can take 6; this should fail if user has 1.00009 which would get rounded up

				respond( code, RefCode.OK, "id", m_uid, "message", msg);  // we return the uid here to be consisten with the live order processing, but it's not really needed since Frontend can only have one Redemption request open at a time

				olog( LogType.REDEEMED, "amount", m_quantity);

				insertRedemption( busd, m_quantity, hash, LiveStatus.Completed); // informational only, don't throw an exception
			}
			catch( Exception e) {
				// update the database so user can try again
				insertRedemption( busd, m_quantity, "", LiveStatus.Failed); // informational only, don't throw an exception
				throw e;
			}
		});
	}
	
	/** return the number of completed trades */
	private int numTrades() throws Exception {
		JsonArray ar = Main.m_config.sqlQuery(
				"select count(uid) from transactions where status = 'COMPLETED' and wallet_public_key = '%s'",
				m_walletAddr.toLowerCase());
		return ar.size() == 0 ? 0 : ar.get(0).getInt("count");
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
							m_config.sendEmail( m_email, "RUSD has been redeemed on Reflection", html);
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
	private void insertRedemption(Busd busd, double rusdPos, String hash, LiveStatus status) {
		Util.wrap( () -> {
			S.out( "inserting or updating record into redemption table with status %s", m_status);

			JsonObject obj = new JsonObject();
			obj.put( "created_at", new MySqlDate() );  // we want created_at to be updated on updates
			obj.put( "uid", m_uid);
			obj.put( "wallet_public_key", m_walletAddr.toLowerCase() );
			obj.put( "stablecoin", busd.name() );
			obj.put( "amount", rusdPos);
			obj.put( "status", status);
			obj.putIf( "blockchain_hash", hash);

			// only allow one "working" redemption at a time
			m_config.sqlCommand( conn -> conn.insertOrUpdate(
					"redemptions", 
					obj,
					"wallet_public_key = '%s' and status in (%s)",
					m_walletAddr.toLowerCase(),
					WorkingStatuses) );
		});
	}

	private void updateRedemption(LiveStatus newStatus) {
		Util.wrap( () -> {
			m_config.sqlCommand( sql -> sql.execWithParams( 
					"update redemptions set status = '%s' where uid = '%s'", newStatus, m_uid) );
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
