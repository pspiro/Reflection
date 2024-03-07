package testcase;

import static fireblocks.Accounts.instance;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Accounts;
import monitor.WalletPanel;
import positions.Wallet;
import reflection.RefCode;
import tw.util.S;

/** You must have some RUSD for these tests to pass */
public class TestRedeem extends MyTestCase {
	
	static String host = "localhost"; // "34.125.38.193";
	
	static String refWallet;
	
	static {
		try {
			readStocks();
			refWallet = Accounts.instance.getAddress("RefWallet");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void mintRusd(String wallet, double amt) throws Exception {
		S.out( "Minting %s RUSD into %s", amt, wallet);

		m_config.rusd()
				.sellStockForRusd( wallet, amt, stocks.getAnyStockToken(), 0)
				.waitForStatus("COMPLETED");
	}
	
	public void testLocked() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered
		
		// make sure we have some BUSD in RefWallet
		if (m_config.busd().getPosition(refWallet) < 10) {
			m_config.busd().mint( refWallet, 20).waitForCompleted();
		}
		
		// mint some RUSD to new wallet 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 5);
		waitForBalance(Cookie.wallet, 4, false); // make sure the new balance will register with the RefAPI
		
		// lock it and redeem; should fail
		lock( 5, System.currentTimeMillis() + Util.DAY);
		redeem();
		assertEquals( RefCode.RUSD_LOCKED, cli.getRefCode() );
		
		// lock part of it; it should redeem only part
		lock( 3, System.currentTimeMillis() + Util.DAY);
		redeem();
		assert200();
		startsWith( "RUSD was partially redeemed", cli.getMessage() );

		// lock it all, but in the past; should succeed
		lock( 5, System.currentTimeMillis() - 10);
		redeem();
		assert200();  // fails here: 14:04:28.513 main REDEMPTION_PENDING - There is already an outstanding redemption request for this wallet; we appreciate your patience

	}
	
	private void lock(int amt, long lockUntil) throws Exception {
		String wallet = Cookie.wallet.toLowerCase();
		JsonObject obj = WalletPanel.createLockObject( wallet, amt, lockUntil);
		m_config.sqlCommand( sql -> sql.insertOrUpdate("users", obj, "wallet_public_key = '%s'", wallet) );
	}

	public void testRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered 

		// make sure we have some BUSD in RefWallet
		double bal = m_config.busd().getPosition(refWallet);
		if (bal < 10) {
			m_config.busd().mint( refWallet, 10).waitForCompleted();
			bal = 10;
		}
		
		// mint an amount of RUSD that should work--high 
		Cookie.setNewFakeAddress( true);
		mintRusd(Cookie.wallet, 9);
		waitForBalance(Cookie.wallet, 1, false); // make sure the new balance will register with the RefAPI
		
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
		waitForBalance(Cookie.wallet, .0001, true);
	}
	
	private void redeem() throws Exception {
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
	}

	public void testExceedMaxAutoRedeem() throws Exception {
		Util.require( !m_config.isProduction(), "No!"); // DO NOT run in production as the crypto sent to these wallets could never be recovered

		// create new wallet with more than the allowed amount of RUSD
		Cookie.setNewFakeAddress(true);
		m_config.rusd().mintRusd( Cookie.wallet, m_config.maxAutoRedeem() + 1, stocks.getAnyStockToken() );

		// fail with INSUFFICIENT_FUNDS due to exceeding maxAutoRedeem value
		cli().postToJson("/api/redemptions/redeem/" + Cookie.wallet, Util.toJson( "cookie", Cookie.cookie).toString() )
			.display();
		assertEquals( RefCode.INSUFFICIENT_FUNDS, cli.getRefCode() );
	}
	
	/** Wait up to 10 sec for Moralis to catch up   // better just to ask hookServer or RefAPI
	 * @throws Exception */
	private void waitForBalance(String address, double bal, boolean lt) throws Exception {
		S.out( "waiting for balance %s bal", lt ? "<" : ">");
		waitFor( 90, () -> { 
			double balance = Wallet.getBalance(address, m_config.rusdAddr() );
			return (lt && balance < bal || !lt && balance > bal);
		});
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
				instance.getId( "RefWallet"), // called by
				m_config.rusdAddr(), // approving
				1000000000); // $1B
	}
	
}
