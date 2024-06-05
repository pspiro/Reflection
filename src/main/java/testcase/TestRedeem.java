package testcase;

import org.json.simple.JsonObject;

import common.Util;
import monitor.BigWalletPanel;
import positions.Wallet;
import reflection.RefCode;
import tw.util.S;

/** You must have some RUSD for these tests to pass */
public class TestRedeem extends MyTestCase {
	
	static String host = "localhost"; // "34.125.38.193";
	
	static String refWallet;
	
	static {
		try {
			refWallet = m_config.refWalletAddr();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testLocked() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered
		
		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			m_config.mintBusd( refWallet, 2000).waitForCompleted();
		}
		
		// mint some RUSD to new wallet 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 5);
		
		// lock it by time and redeem; should fail
		lock( 5, System.currentTimeMillis() + Util.DAY, 0);
		redeem();
		assertEquals( RefCode.RUSD_LOCKED, cli.getRefCode() );
		S.out( cli.getMessage() );
		
		// lock it by required transactions; should fail
		lock( 5, System.currentTimeMillis() - Util.DAY, 2);
		redeem();
		assertEquals( RefCode.RUSD_LOCKED, cli.getRefCode() );
		S.out( cli.getMessage() );
		
		// lock part of it; it should redeem only part
		lock( 3, System.currentTimeMillis() + Util.DAY, 0);
		redeem();
		assert200();
		startsWith( "RUSD was partially redeemed", cli.getMessage() );
	}
	
	public void testLockedInPast() throws Exception {
		// lock it all, but in the past; should succeed
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 5);
		lock( 5, System.currentTimeMillis() - 10, 0);
		redeem();
		assert200();
	}
	
	private void lock(int amt, long lockUntil, int requiredTrades) throws Exception {
		String wallet = Cookie.wallet.toLowerCase();
		JsonObject lockObj = BigWalletPanel.createLockObject( wallet, amt, lockUntil, requiredTrades);
		m_config.sqlCommand( sql -> sql.insertOrUpdate("users", lockObj, "wallet_public_key = '%s'", wallet) );
	}

	public void testRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered 

		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			m_config.mintBusd( refWallet, 2000)
					.waitForCompleted();
		}
		
		// mint an amount of RUSD that should work--high 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 9);
		
		// redeem RUSD, pass
		S.out( "sending redemption request");
		redeem();
		assert200();

		// second one should fail w/ REDEMPTION_PENDING
		S.sleep(200);
		S.out( "sending dup redemption request");
		redeem();
		assertEquals( RefCode.REDEMPTION_PENDING, cli.getRefCode() );

		waitForRedeem(Cookie.wallet);
		waitForRusdBalance(Cookie.wallet, .0001, true);
	}
	
	private void redeem() throws Exception {
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
	}

	public void testExceedMaxAutoRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered

		// create new wallet with more than the allowed amount of RUSD
		Cookie.setNewFakeAddress(true);
		mintRusd( Cookie.wallet, m_config.maxAutoRedeem() + 1);

		// fail with INSUFFICIENT_FUNDS due to exceeding maxAutoRedeem value
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
		assertEquals( RefCode.OVER_REDEMPTION_LIMIT, cli.getRefCode() );
	}
	
	public void testCheckBalance() throws Exception {
		S.out( "Balance: " + Wallet.getBalance(Cookie.wallet, m_config.rusdAddr() ) );
	}
		
	/** can't use waitFor() here because we want to stop when there is any non-null status */
	private void waitForRedeem(String wallet) throws Exception {
		S.out( "waiting for redeem via live order system");
		
		S.sleep(100); // give it time to get into the map
		while (true) {
			JsonObject rusd = cli().get("/api/mywallet/" + Cookie.wallet).readJsonObject().getArray("tokens").getJsonObj(0);
			String status = rusd.getString("status");
			if (S.isNotNull(status) ) {
				S.out( rusd.getString("text"));
				assertEquals( "Completed", status);
				break;
			}
			//S.out( rusd.getInt("progress") );
			S.sleep(1000);
		}
	}

	public void testFailAddress() throws Exception {
		// invalid address (wrong length)
		cli().addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + Cookie.wallet + "a");
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
		
		// wrong address (must match cookie)
		String wallet = ("0xaaa" + Cookie.wallet).substring(0, 42);
		cli().addHeader("Cookie", Cookie.cookie)
			.get("/api/redemptions/redeem/" + wallet);
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.VALIDATION_FAILED, cli.getRefCode() );
	}
	
	public void testFailNoCookie() throws Exception {
		cli().get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "fail: " + cli.readString() );
		assertEquals(400, cli.getResponseCode() );
	}

	public void test() throws Exception {
		m_config.busd().approve( 
				m_config.refWalletKey(),
				m_config.rusdAddr(), // approving
				1000000000); // $1B
	}
	
}
