package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import reflection.RefCode;
import tw.util.S;
import web3.Busd;
import web3.Rusd;
import web3.StockToken;

public class TestFbOrders extends MyTestCase {
	// test all possible orders and a success of buy w/ BUSD, buy w/ RUSD, and sell
	
	static String bobKey;
	static String bobAddr;
	static StockToken stock;
	static Busd busd;
	static Rusd rusd;

	static {		
		try {
			bobKey = Util.createPrivateKey();
			bobAddr = Util.getAddress(bobKey);

			S.out( "bobAddr = %s", bobAddr);
			S.out( "bobKey = %s", bobKey);

			busd = m_config.busd();
			rusd = m_config.rusd();

			showAmounts("pre-run");
		}
		catch( Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void testInsufficientFundsSell() throws Exception {
		// write this
	}

	/** This test will fail. To get it to pass, update LiveOrder.updateFrom() to check for 
	 *  insufficient crypto or insufficient approved amount */
	public void testInsufficientFundsBuy() throws Exception {
		S.out( "-----testInsufficientFundsBuy");
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting insuf. funds");
		
		// get current BUSD balance
		double bal = m_config.busd().getPosition( bobAddr);
		double maxQty = bal / (TestOrder.curPrice+3);  // this is the max we could buy w/ the current balance
		
		// buy more
		JsonObject obj = TestOrder.createOrderWithOffset( "BUY", maxQty + 1, 3);
		postOrderToObj(obj);
		assertEquals( RefCode.INSUFFICIENT_STABLECOIN, cli.getRefCode() );
	}
	
	/** this test fails? and we must fix it */
//	public void testApproval() throws Exception {
//		gasUpBob();
//		busd.approve( bobKey, rusd.address(), 1).waitForCompleted();
//		S.sleep(1000);
//		S.out( m_config.busd().getApprovedAmt( bobAddr, rusd.address() ) );
//		assertEquals( 1.0, m_config.busd().getApprovedAmt( bobAddr, rusd.address() ) );
//		waitFor( 15, () -> m_config.busd().getApprovedAmt( bobAddr, rusd.address() ) == 1);
//	}

	/** This test will fail. To get it to pass, update LiveOrder.updateFrom() to check for 
	 *  insufficient crypto or insufficient approved amount. It's only worthwhile
	 *  if we see this happening in real life  */
	public void testInsufficientAllowance() throws Exception {
		// set wallet
		S.out( "-----testNonApproval");
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting non-approval");

		// mint 2000 BUSD for user Bob
		S.out( "**minting 2000");
		m_config.mintBusd( bobAddr, 2000)
			.waitForReceipt();
		waitForBalance(bobAddr, busd.address(), 2000, false); // not return json
		
		gasUpBob();
		
		// let bob approve .49 BUSD spending by RUSD
		S.out( "**approving .49");
		busd.approve( bobKey, rusd.address(), .49).waitForReceipt();
		waitFor( 30, () -> m_config.busd().getAllowance( bobAddr, rusd.address() ) > .48);
		showAmounts("updated amounts");
		
		String id = postOrderToObj( TestOrder.createOrder3( "BUY", 1, TestOrder.curPrice + 3, busd.name() ) ).getString("id");
		assert200();
		
		// use this if we change the logic in RefAPI to check allowance before sending 
		// the transaction; that would be preferable as long as it works
		//assertEquals( RefCode.INSUFFICIENT_ALLOWANCE, cli.getRefCode() );

		// note we will get back some messages and then the error
		// waiting for live order to fail
		S.out( "waiting for status of order " + id);
		waitFor( 30, () -> {
			JsonObject ret = getLiveMessage(id);
			if (ret != null) {
				ret.display();
				if (ret.getString("type").equals("error") ) {
					startsWith( "The approved amount", ret.getString("text") );
					return true;
				}
			}
			return false;
		});
	}

	/** There must be a valid profile for Bob for this to work */
	public void testFillWithFb() throws Exception {  // always fails the second time!!!
		gasUpBob();

		// set wallet
		S.out( "-----testFillWithFb");
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting amounts");
		
		// give bob a valid user profile
		JsonObject json = TestProfile.createValidProfile();
		json.put( "email", "test@test.com"); // recognized by RefAPI, non-production only
		MyClient.postToJson( "http://localhost:" + port + "/api/update-profile", json.toString() );
		
		// let it pass KYC
		m_config.sqlCommand( sql -> sql.execWithParams( 
				"update users set kyc_status = 'VERIFIED' where wallet_public_key = '%s'",  
				bobAddr));

		// mint BUSD for user Bob
		S.out( "**minting 2000");
		m_config.mintBusd( bobAddr, 2000).waitForReceipt();
		waitForBalance( bobAddr, m_config.busd().address(), 2000, false);
		
		gasUpBob();
		
		// let bob approve buying with BUSD; you must wait for this
		S.out( "**approving 20000");
		busd.approve(
				bobKey,
				rusd.address(),      // I saw this hang forever
				2000
				).waitForReceipt();
		waitFor( 120, () -> busd.getAllowance( bobAddr, rusd.address()) > 1999);
		showAmounts("updated amounts");

		// submit order
		JsonObject obj = TestOrder.createOrder3( "BUY", 1, TestOrder.curPrice + 3, busd.name() );
		S.out( "**Submitting: " + obj);
		JsonObject map = postOrderToObj(obj);
		assert200();

		// show uid
		String uid = map.getString("id");  // 5-digit code
		assertTrue( S.isNotNull(uid) );
		S.out( "Submitted order with uid %s", uid);

		// wait for order to complete
		waitFor( 120, () -> {
			JsonObject liveOrders = getAllLiveOrders(bobAddr);
			S.out( liveOrders);

			JsonObject msg = liveOrders.getArray("messages").find("id", uid);
			JsonObject order = liveOrders.getArray("orders").find("id", uid);
			
			// order is still working
			if (order != null) {
				if (msg != null) {
					S.out( msg);
				}
			}
			else if (msg != null) {
				S.out("Completed: " + msg);
				assertEquals( "message", msg.getString("type"));
				startsWith( "Bought", msg.getString("text") );
				return true;
			}
			assertTrue(order != null);
			return false;
		});
		
		// fetch most recent transaction from database
		JsonArray ar = m_config.sqlQuery( "select * from transactions order by created_at desc limit 1");
		assertTrue( ar.size() > 0);
		
		JsonObject rec = ar.get(0);
		S.out(rec);
		assertEquals( "COMPLETED", rec.getString("status") );
		assertEquals( 1.0, rec.getDouble("quantity") );
	}
	
	/** There must be a valid profile for Bob for this to work
	 * 
	 *  This only works when run by itself; don't know why */
	public void testFillRusd() throws Exception {  // always fails the second time!!!
		// set wallet
		S.out( "-----testFillRusd");
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting amounts");
		
		// give bob a valid user profile
		JsonObject json = TestProfile.createValidProfile();
		json.put( "email", "test@test.com"); // recognized by RefAPI, non-production only
		MyClient.postToJson( "http://localhost:" + port + "/api/update-profile", json.toString() );
		
		// let it pass KYC
		m_config.sqlCommand( sql -> sql.execWithParams( 
				"update users set kyc_status = 'VERIFIED' where wallet_public_key = '%s'",  
				bobAddr));

		// mint RUSD for user Bob
		S.out( "**minting 2000");
		rusd.mintRusd( bobAddr, 2000, chain.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance( bobAddr, 2000, false);
		
		// submit order
		JsonObject obj = TestOrder.createOrder3( "BUY", 1, TestOrder.curPrice + 3, rusd.name() );
		S.out( "**Submitting: " + obj);
		JsonObject map = postOrderToObj(obj);
		assert200();

		// show uid
		String uid = map.getString("id");  // 5-digit code
		assertTrue( S.isNotNull(uid) );
		S.out( "Submitted order with uid %s", uid);

		// wait for order to complete
		waitFor( 120, () -> {
			JsonObject liveOrders = getAllLiveOrders(bobAddr);
			S.out( liveOrders);

			JsonObject msg = liveOrders.getArray("messages").find("id", uid);
			JsonObject order = liveOrders.getArray("orders").find("id", uid);
			
			// order is still working
			if (order != null) {
				if (msg != null) {
					S.out( msg);
				}
			}
			else if (msg != null) {
				S.out("Completed: " + msg);
				assertEquals( "message", msg.getString("type"));
				startsWith( "Bought", msg.getString("text") );
				return true;
			}
			assertTrue(order != null);
			return false;
		});
		
		// fetch most recent transaction from database
		JsonArray ar = m_config.sqlQuery( "select * from transactions order by created_at desc limit 1");
		assertTrue( ar.size() > 0);
		
		JsonObject rec = ar.get(0);
		S.out(rec);
		assertEquals( "COMPLETED", rec.getString("status") );
		assertEquals( 1.0, rec.getDouble("quantity") );
	}
	
	/** we can be short RUSD up to .02 and the RefAPI will allow it; see code 
	 *  in OrderTransaction.requireSufficientCrypto()  */ 
	public void testALitleShort() throws Exception {
		S.out( "-----testaLittleShort  price=%s", TestOrder.curPrice);

		// set wallet
		Cookie.setNewFakeAddress(true);
		
		// create order
		JsonObject obj = TestOrder.createOrder3( "BUY", 1, TestOrder.curPrice + 3, rusd.name() );
		double amount = obj.getDouble( "amount");

		// mint a little less RUSD than needed
		S.out( "**minting 100");
		rusd.mintRusd( Cookie.wallet, amount - .01, chain.getAnyStockToken() ).waitForReceipt();
		waitForRusdBalance( Cookie.wallet, amount - .01, false);

		// submit order
		S.out( "**Submitting: " + obj);
		postOrderToObj(obj);
		assert200();
	}
	
	
	/** The owner wallet must have some gas for this to work */
	private void gasUpBob() throws Exception {
		// give bob some gas?
		if (node().getNativeBalance(bobAddr) < .01) {  // .02 works, what about 1?
			S.out( "gassing up bob");
			m_config.chain().blocks().transfer( m_config.ownerKey(), bobAddr, .01)
					.waitForReceipt();
		}
	}

	static void showAmounts(String str) throws Exception {
		S.out( "%s  approved=%s  USDC=%s  RUSD=%s",
				str,
				m_config.busd().getAllowance(bobAddr, m_config.rusdAddr() ),
				m_config.busd().getPosition(bobAddr),
				m_config.rusd().getPosition(bobAddr) );
	}
	
	public void testTotalSupply() throws Exception {
		double i = m_config.rusd().queryTotalSupply();
		assertTrue( i > 0);
	}
}

// you must catch nonce error and reset nonce
// you must update Monitor to never use admin1, only Reflection can have access to that
// two apps cannot use the same account
// you need synchronization on using admin1 from RefAPI