package testcase.web3;

import common.Util;
import positions.Wallet;
import test.MyTimer;
import testcase.MyTestCase;
import tw.util.S;
import web3.RetVal;
import web3.StockToken;

/** Test smart contracts */
public class TestRusd extends MyTestCase {
	static String someKey = "bdc76b290316a836a01af129884bdf9a1977b81ae5a7a0f1d8b5ded4d9dcee4d";
	
	static {
		readStocks();
	}
	
	public void testAddOrRemovePass() throws Exception {
		S.out( "adding admin to pass");
		m_config.rusd().addOrRemoveAdmin(
				m_config.ownerKey(),
				Util.createFakeAddress(),
				true).waitForHash();
	}

	public void testAddOrRemoveFail() throws Exception {
		try {
			S.out( "adding admin to fail");
			RetVal ret = m_config.rusd().addOrRemoveAdmin(
					someKey,
					Util.createFakeAddress(),
					true);
			ret.waitForHash();
			assertTrue( false);  // should not come here
		}
		catch( Exception e) {
			S.out( e.getMessage() ); // expected
		}
	}
	
	/** This executes in 8s to 13s as-is vs 33s if you wait for each transaction */
	public void testAdmin() throws Exception {
		MyTimer t = new MyTimer();
		t.next("start");
		
		String user = Util.createFakeAddress();
		
		// mint 100 rusd
		S.out( "minting rusd");
		m_config.rusd().mintRusd( user, 100, stocks.getAnyStockToken() )
				; //.waitForHash();
		
		// buy stock
		S.out( "buying stock");
		StockToken stock = stocks.getAnyStockToken();
		m_config.rusd().buyStockWithRusd( user, 20, stock, 10)
				; //.waitForHash();
		
		// sell stock
		S.out( "selling stock");  // failing with same nonce
		m_config.rusd().sellStockForRusd( user, 10, stock, 5)
				; //.waitForHash();
		
		// mint busd into refwallet so user can redeem (anyone can call this, must have matic)
		S.out( "minting busd");
		m_config.busd().mint( m_config.refWalletAddr(), 80)
				; //.waitForHash();

		// user has 90 redeem 80, left with 10
		S.out( "redeeming rusd");
		m_config.rusd().sellRusd( user, m_config.busd(), 80)  // failed insuf. allowance
				.waitForHash();
		
		t.next("checkpoint");
		
		Wallet wallet = new Wallet( user);
		assertEquals( 5.0, wallet.getBalance( stock.address() ) );
		assertEquals( 10.0, wallet.getBalance( m_config.rusd().address() ) );
		
		t.done();
	}
		
	// 13x + 678 ms with no waiting vs 33 sec with waiting for each one
	// 8 sec + 705 ms
	
		// remove admin
//		rusd.addOrRemoveAdmin(admin, false)
//			.waitForCompleted();
		
		// buy a stock token - fail
//		rusd.buyStockWithRusd(dead, 1, st, 1)
//			.waitForStatus("FAILED");
}
