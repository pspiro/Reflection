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
				Util.createFakeAddress(),
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
	
	/** This executes in 8s to 13s as-is vs 33s if you wait for each transaction */
	public void testAdmin() throws Exception {
		MyTimer t = new MyTimer();
		t.next("start");
		
		String user = Util.createFakeAddress();
		
		// for Refblocks, you don't need the waitForHash or waitForCompleted or anything
		// for Fireblocks, you have to wait
		
		// mint 100 rusd
		S.out( "***minting rusd");
		mintRusd( user, 100);
		
		// buy stock
		StockToken stock = stocks.getAnyStockToken();
		S.out( "***buying stock %s", stock.address() );
		m_config.rusd().buyStockWithRusd( user, 20, stock, 10)
				.waitForReceipt();
		
		// sell stock
		S.out( "***selling stock");  // failing with same nonce
		m_config.rusd().sellStockForRusd( user, 10, stock, 5)
				.waitForReceipt();
		
		// mint busd into refwallet so user can redeem (anyone can call this, 
		// must have matic)
		S.out( "***minting busd");

		// user has 90 redeem 80, left with 10
		S.out( "***redeeming rusd");
		m_config.rusd().sellRusd( user, m_config.busd(), 80)  
				.waitForReceipt(); // if fails, check for insuf. allowance
		
		t.next("***checkpoint");
		
		// for refblocks, the balance is there; for fireblocks, you need 
		// to wait for the positions
		waitForBalance(user, stock.address(), 5, false);
		waitForBalance(user, m_config.rusdAddr(), 10, false);
		
		t.done();
	}
	
	public void testTrouble() throws Exception {
		String user = "0xd60b716d9b511d21087ee02351a6b4e9ec90dcda"; //Util.createFakeAddress();
		
		// mint 100 rusd
		S.out( "***minting rusd");
		m_config.rusd().mintRusd( user, 100, stocks.getAnyStockToken() )
				; //.waitForHash();

		S.out( "  rusd balance = " + m_config.rusd().getPosition( user) );
		
		
		// buy stock
		StockToken stock = stocks.getAnyStockToken();
		S.out( "***buying stock %s", stock.address() );
		m_config.rusd().buyStockWithRusd( user, 20, stock, 10)
				.waitForReceipt();
		S.out( "  stock balance = " + stock.getPosition( user) );
		

//		
//		// sell stock
//		S.out( "***selling stock");  // failing with same nonce
//		m_config.rusd().sellStockForRusd( user, 10, stock, 5)
//				; //.waitForHash();
//		
//		// mint busd into refwallet so user can redeem (anyone can call this, must have matic)
//		S.out( "***minting busd");
//		m_config.busd().mint( m_config.refWalletAddr(), 80)
//				; //.waitForHash();
//
//		// user has 90 redeem 80, left with 10
//		S.out( "***redeeming rusd");
//		m_config.rusd().sellRusd( user, m_config.busd(), 80)  // failed insuf. allowance
//				.waitForHash();
//		
//		t.next("***checkpoint");
//		
//		Wallet wallet = new Wallet( user);
//		S.out( "balance is %s", wallet.getBalance( stock.address() ) );
//		assertEquals( 5.0, wallet.getBalance( stock.address() ) );
//		assertEquals( 10.0, wallet.getBalance( m_config.rusd().address() ) );
//		
//		t.done();
	}
		// buy a stock token - fail
//		rusd.buyStockWithRusd(dead, 1, st, 1)
//			.waitForStatus("FAILED");
		
}
// this is a mystery: how can I call buyStock and pass the address of a
// non-existent stock token, it the transaction succeeds
// 0x098262f4d177565409450602d90491e523d04ec5a0218847525e2c687771ee33
// called with stock token addr 0xf000b01e40ffeef7d6b6c6a1365a8a9885027ece
// very strange that if you make a smart contract call that will fail,
// sometimes waitForHash() returns true and sometimes false