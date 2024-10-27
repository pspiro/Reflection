package testcase;

import org.json.simple.JsonObject;

import common.Util;
import monitor.WalletPanel;
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
	// FAILING WITH NONCE ERROR!!!!!!!!!!!!!!!
	public void testLocked() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered
		S.out( "***testLocked");
		
		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			m_config.mintBusd( refWallet, 2000).waitForReceipt();
		}
		
		// mint some RUSD to new wallet 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 5);
		
		// lock it by time and redeem; should fail
		lock( 5, System.currentTimeMillis() + Util.DAY, 0);
		redeem( 5);
		assertEquals( RefCode.RUSD_LOCKED, cli.getRefCode() );
		S.out( cli.getMessage() );
		
		// lock it by required transactions; should fail
		lock( 5, System.currentTimeMillis() - Util.DAY, 2);
		redeem( 5);
		assertEquals( RefCode.RUSD_LOCKED, cli.getRefCode() );
		S.out( cli.getMessage() );
		
		// lock part of it; it should redeem only part
		lock( 3, System.currentTimeMillis() + Util.DAY, 0);
		redeem( 5);
		assert200();
		startsWith( "RUSD was partially redeemed", cli.getMessage() );
	}
	
	public void testLockedInPast() throws Exception {
		S.out( "***testLockedInPast");
		
		// lock it all, but in the past; should succeed
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 5);
		lock( 5, System.currentTimeMillis() - 10, 0);
		redeem( 5);
		assert200();
	}
	
	private void lock(int amt, long lockUntil, int requiredTrades) throws Exception {
		String wallet = Cookie.wallet.toLowerCase();
		JsonObject lockObj = WalletPanel.createLockObject( wallet, amt, lockUntil, requiredTrades);
		m_config.sqlCommand( sql -> sql.insertOrUpdate("users", lockObj, "wallet_public_key = '%s'", wallet) );
	}

//	public void testredeem( 5) throws Exception {
//		String wal = "0xcb6c2EDBb986ef14B66E094787245350b69EA5Ec";
//		Cookie.setWalletAddr(wal);
//		S.out( "**approved=%s", m_config.getApprovedAmt() );
//		S.out( "**rusdBal=%s", m_config.rusd().getPosition(wal));
//		S.out( "**busdBal=%s", m_config.busd().getPosition(m_config.refWalletAddr()));
//		
//		S.out( "sending redemption request to succeed");
//		m_config.rusd().sellRusd(wal, m_config.busd(), 3)
//			.displayHash();
////		redeem( 5);
//		//assert200();
//	}
	
	// test is failing; here's what you need to do: redeploy busd with lots of error info including the
	// balance
	public void testPartial() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered 
		S.out( "***testPartial");

		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			S.out( "minting");
			m_config.mintBusd( refWallet, 2000)
					.waitForReceipt();
		}
		
		// mint an amount of RUSD
		S.out( "minting");
		//Cookie.setNewFakeAddress( true);
		Cookie.setWalletAddr( "0xDD250c8360f4A892A2D099220B39CF46b8A688AE");
//		mintRusd(Cookie.wallet, 7);
		
		m_config.rusd().sellRusd(
				Cookie.wallet, 
				m_config.busd(), 
				3).waitForReceipt(); // rounds to 4 decimals, but RUSD can take 6; this should fail if user has 1.00009 which would get rounded up

		System.exit(0);
		
		// fail, amount too low
		S.out( "fail");
		redeem( 2);
		assertEquals( RefCode.INVALID_REQUEST, cli.getRefCode() );
	
		// succeed
		S.out( "succeed");
		redeem( 5);
		assert200();
		waitForRusdBalance( Cookie.wallet, 2, true);
		
		// do the rest
		S.out( "succeed");
		redeem( 3);
		assert200();
		waitForRusdBalance( Cookie.wallet, 0, true);
		

	}
	
	/** This test is failing in dev3 and you couldn't figure it out.
	 *  to fix it, show it to Jitin, or re-write the busd class to print out
	 *  more infomation in the error message, or put BUSD and RUSD code all into chat
	 *  
	 * @throws Exception
	 */
	public void testInsufAndRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered 
		S.out( "***testRedeem");

		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			m_config.mintBusd( refWallet, 2000)
					.waitForReceipt();
		}
		
		// mint an amount of RUSD that should work--high 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 9);
		
		// fail, too low
		redeem( 4);
		assert400();

		// this doesn't work; for some reason
		// clear approved amount
		S.out( "clearing allowance");
		m_config.busd().approve(
				m_config.refWalletKey(), m_config.rusdAddr(), 1).waitForReceipt(); // $1M
		S.out( "approved: " + m_config.getApprovedAmt( chain() ) );

		// redeem RUSD, fail due to allowance
		S.out( "sending redemption request to fail");
		redeem( 5);
		assert400();

		// restore approved amount
		S.out( "restoring allowance");
		m_config.busd().approve(
				m_config.refWalletKey(), m_config.rusdAddr(), 1000000000).waitForReceipt(); // $1M
		S.out( "approved: " + m_config.getApprovedAmt(chain() ) );
		
		
		// wait for it to solidify
		S.out( "waiting 5 sec");
		S.sleep( 5000);

		// redeem RUSD, pass
		S.out( "sending redemption request to succeed");
		redeem( 5);
		assert200();

		// second one should fail w/ REDEMPTION_PENDING
		S.sleep(200);
		S.out( "sending dup redemption request to fail");
		redeem( 5);
		assertEquals( RefCode.REDEMPTION_PENDING, cli.getRefCode() );

		waitForRedeem(Cookie.wallet);
		waitForRusdBalance(Cookie.wallet, .0001, true);
	}

	private void redeem(double amount) throws Exception {
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( 
				"cookie", Cookie.cookie,
				"quantity", amount)
			.toString() )
		.display();
	}

	public void testExceedmaxAutoRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered
		S.out( "***testExceedMax");

		// create new wallet with more than the allowed amount of RUSD
		Cookie.setNewFakeAddress(true);
		mintRusd( Cookie.wallet, m_config.maxAutoRedeem() + 1);

		// fail with INSUFFICIENT_FUNDS due to exceeding maxAutoRedeem value
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
		assertEquals( RefCode.OVER_REDEMPTION_LIMIT, cli.getRefCode() );
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
		S.out( "***testFailAddr");

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
		S.out( "***testFailNoCookie");
		cli().get("/api/redemptions/redeem/" + Cookie.wallet);
		S.out( "fail: " + cli.readString() );
		assertEquals(400, cli.getResponseCode() );
	}

	public void testAppr() throws Exception {
		S.out( "***testAppr");
		
		m_config.busd().approve( 
				m_config.refWalletKey(),
				m_config.rusdAddr(), // approving
				1000000000); // $1B
	}
	
}
