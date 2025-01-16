package testcase.web3;

import common.Util;
import test.MyTimer;
import testcase.MyTestCase;
import tw.util.S;
import web3.RetVal;
import web3.StockToken;

/** Test smart contracts */
public class TestRusd extends MyTestCase {
	static String someKey = "bdc76b290316a836a01af129884bdf9a1977b81ae5a7a0f1d8b5ded4d9dcee4d";
	
	public void testAddOrRemovePass() throws Exception {
		S.out( "***adding admin to pass");
		m_config.rusd().addOrRemoveAdmin(
				m_config.ownerKey(),
				//Util.createFakeAddress(),
				chain().params().sysAdminAddr(),
				true).waitForReceipt();
	}

	public void testAddOrRemoveFail() throws Exception {
		try {
			S.out( "***adding admin to fail");
			RetVal ret = m_config.rusd().addOrRemoveAdmin(
					someKey,
					Util.createFakeAddress(),
					true);
			ret.waitForReceipt();
			assertTrue( false);  // should not come here
		}
		catch( Exception e) {
			S.out( e.getMessage() ); // expected
		}
	}
	
	/** This test is working on Sepolia as of 1/16/25 */
	public void testBuySellRedeem() throws Exception {
		String userKey = Util.createPrivateKey();
		String user = Util.getAddress( userKey);

		MyTimer t = new MyTimer();
		t.next("starting with user " + user);
		
		// buy 10 stock for $20 RUSD, bal $80
		StockToken stock = chain.getAnyStockToken();
		t.next( "***buying 10 stock with $20 RUSD", stock.address() );
		mintRusd( user, 100);
		m_config.rusd().buyStockWithRusd( user, 20, stock, 10);

		// buy 10 stock for $20 BUSD, bal $80
		t.next( "***buying 10 stock with $20 BUSD", stock.address() );
		mintBusd( user, 100);
		chain().node().transfer( chain().params().admin1Key(), user, .005).waitForReceipt(); // give some gas so the approve will go through
		chain().busd().approve( userKey, chain.rusd().address(), 100).waitForReceipt();
		m_config.rusd().buyStock(user, chain().busd(), 20, stock, 10);
		
		// sell 5 stock for $10 RUSD, bal $90
		t.next( "***selling 5 stock for $10 RUSD");
		m_config.rusd().sellStockForRusd( user, 10, stock, 5);

		// user has 90 RUSD, redeem 60, left with 30
		t.next( "***redeeming rusd");
		mintBusd( m_config.refWalletAddr(), 100);
		S.out( "RUSD balance user: " + m_config.rusd().getPosition(user));
		S.out( "BUSD balance refWallet: " + m_config.busd().getPosition(chain().params().refWalletAddr()));
		S.out( "BUSD allowance refWallet: " + m_config.busd().getAllowance(m_config.params().refWalletAddr(), m_config.rusd().address() ) );
		m_config.rusd().sellRusd( user, m_config.busd(), 60)
				.waitForReceipt(); // if fails, check for insuf. allowance
		t.done();

		// check balances
		waitForRusdBalance(user, 30, true);
		waitForBalance(user, stock.address(), 15, true);
	}
}
// this is a mystery: how can I call buyStock and pass the address of a
// non-existent stock token, it the transaction succeeds
// 0x098262f4d177565409450602d90491e523d04ec5a0218847525e2c687771ee33
// called with stock token addr 0xf000b01e40ffeef7d6b6c6a1365a8a9885027ece
// very strange that if you make a smart contract call that will fail,
// sometimes waitForHash() returns true and sometimes false