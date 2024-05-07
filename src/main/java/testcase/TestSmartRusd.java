package testcase;

import common.Util;
import positions.Wallet;
import tw.util.S;
import web3.StockToken;

/** Test smart contracts */
public class TestSmartRusd extends MyTestCase {
	static String someKey = "bdc76b290316a836a01af129884bdf9a1977b81ae5a7a0f1d8b5ded4d9dcee4d";
	
	public void testAddOrRemovePass() throws Exception {
		m_config.rusd().addOrRemoveAdmin(
				m_config.ownerKey(),
				Util.createFakeAddress(),
				true);
	}

	public void testAddOrRemoveFail() throws Exception {
		try {
			m_config.rusd().addOrRemoveAdmin(
					someKey,
					Util.createFakeAddress(),
					true);
			assertTrue( false);  // should not come here
		}
		catch( Exception e) {
			// expected
			S.out( e.getMessage() );
		}
	}


	public void testAdmin() throws Exception {
		String user = Util.createFakeAddress();
		
		// mint 100 rusd
		m_config.rusd().mintRusd( user, 100, stocks.getAnyStockToken() )
				.waitForHash();
		
		// buy stock
		StockToken tok = stocks.getAnyStockToken();
		m_config.rusd().buyStockWithRusd( user, 20, tok, 10)
				.waitForHash();
		
		// sell stock
		m_config.rusd().sellStockForRusd( user, 10, tok, 5)
				.waitForHash();
		
		// mint rusd into refwallet
		m_config.busd().mint( user, m_config.refWalletAddr(), 90)
				.waitForHash();

		// redeem rusd
		m_config.rusd().sellRusd( user, m_config.busd(), 80)
				.waitForHash();
		
		Wallet wallet = new Wallet( user);
		assertEquals( 5, wallet.getBalance( tok.address() ) );
		assertEquals( 10, wallet.getBalance( m_config.rusd().address() ) );
	}
		
		// remove admin
//		rusd.addOrRemoveAdmin(admin, false)
//			.waitForCompleted();
		
		// buy a stock token - fail
//		rusd.buyStockWithRusd(dead, 1, st, 1)
//			.waitForStatus("FAILED");
}
