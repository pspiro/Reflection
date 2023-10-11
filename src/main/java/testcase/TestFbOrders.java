package testcase;

import java.util.Date;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Busd;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import reflection.RefCode;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class TestFbOrders extends MyTestCase {
	// test all possible orders and a success of buy w/ BUSD, buy w/ RUSD, and sell
	
	static String bobAddr;
	static StockToken stock;
	static Busd busd;
	static Rusd rusd;

	static {		
		try {
			bobAddr = accounts.getAddress("Bob");
			S.out( "bobAddr = %s", bobAddr);
			
			// create Wallet instead and use that
			showAmounts("pre-run");
			
			GTable tab = new GTable( NewSheet.Reflection, m_config.symbolsTab(), "TokenSymbol", "TokenAddress");
			stock = new StockToken( tab.get( "GOOG") );
			busd = m_config.busd();
			rusd = m_config.rusd();
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
		Cookie.setNewWallet(bobAddr);
		showAmounts("starting insuf. funds");
		
		cli().get("/api/mywallet/" + bobAddr);
		double bal = cli.readJsonObject().getArray("tokens").find( "name", "USDC").getDouble("balance");
		double maxQty = bal / (TestOrder.curPrice+3);  // this is the max we could buy w/ the current balance
		
		JsonObject obj = TestOrder.createOrder( "BUY", maxQty + 1, 3);
		obj.remove("noFireblocks");
		
		JsonObject map = postOrderToObj(obj);
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		while(true) {
			JsonObject ret = getLiveMessage2(map.getString("id"));
			if (ret != null) {
				S.out(ret);
				assertEquals( "error", ret.getString("type"));
				startsWith( "The stablecoin balance", ret.getString("text") );
				break;
			}
			S.sleep(1000);
		}
	}

	/** This test will fail. To get it to pass, update LiveOrder.updateFrom() to check for 
	 *  insufficient crypto or insufficient approved amount. It's only worthwhile
	 *  if we see this happening in real life  */
	public void testNonApproval() throws Exception {
		S.out( "-----testNonApproval");
		Cookie.setNewWallet(bobAddr);
		showAmounts("starting non-approval");

		// mint BUSD for user Bob
		// mint BUSD for user Bob
		S.out( "**minting 2000");
		busd.mint( bobAddr, 2000)  // I don't think this is necessary but I saw it fail without this
			.waitForHash();
		
		// approve too little
		S.out( "**approving .49");
		busd.approve(
				accounts.getId( "Bob"),
				rusd.address(),
				.49).waitForCompleted();
		
		showAmounts("updated amounts");
		
		JsonObject obj = TestOrder.createOrder( "BUY", .1, 3);
		obj.remove("noFireblocks");
		
		JsonObject map = postOrderToObj(obj);
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		while(true) {
			JsonObject ret = getLiveMessage2(map.getString("id"));
			if (ret != null) {
				S.out(ret);
				assertEquals( "error", ret.getString("type"));
				startsWith( "The approved amount", ret.getString("text") );
				break;
			}
			S.sleep(1000);
		}
	}

	public void testFillWithFb() throws Exception {  // always fails the second time!!!
		S.out( "-----testFillWithFb");
		Cookie.setNewWallet(bobAddr);
		showAmounts("starting amounts");
		
		// mint BUSD for user Bob
//		S.out( "**minting 2000");
//		busd.mint(
//				accounts.getId( "Admin1"),
//				bobAddr,
//				2000).waitForHash();  // I don't think this is necessary but I saw it fail without this
//		
//		// user to approve buying with BUSD; you must wait for this
//		S.out( "**approving 20000");
//		busd.approve(
//				accounts.getId( "Bob"),
//				rusd.address(),
//				2000).waitForCompleted();
//
//		showAmounts("updated amounts");


		//double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() );

		final String now = Util.yToS.format( new Date() );
		
		JsonObject obj = TestOrder.createOrder( "BUY", 1, 3);
		obj.remove("noFireblocks");
		
		S.out( "**Submitting: " + obj);

		JsonObject map = postOrderToObj(obj);
		assert200();
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		String uid = map.getString("id");  // 5-digit code
		assertTrue( S.isNotNull(uid) );
		S.out( "Submitted order with uid %s", uid);
		
		while(true) {
			JsonObject liveOrders = getAllLiveOrders(bobAddr);
			S.out( liveOrders);

			JsonObject msg = liveOrders.getArray("messages").find("id", uid);
			JsonObject order = liveOrders.getArray("orders").find("id", uid);
			
			if (msg != null) {
				S.out("Completed: " + msg);
				assertEquals( "message", msg.getString("type"));
				startsWith( "Bought", msg.getString("text") );
				break;
			}
			
			assertTrue(order != null);

			S.sleep(1000);
		}
		
		JsonArray ar = m_config.sqlQuery( conn -> conn.queryToJson("select * from transactions where created_at > '%s'", now) );
		assertTrue( ar.size() > 0);
		JsonObject rec = ar.get(0);
		S.out(rec);
		assertEquals( "CONFIRMING", rec.getString("status") );  // should later change to COMPLETED. pas
		assertEquals( 1.0, rec.getDouble("quantity") );
	}
	
	static void showAmounts(String str) throws Exception {
		S.out( "%s  approved=%s  USDC=%s  RUSD=%s  StockToken=%s",
				str,
				m_config.busd().getAllowance(bobAddr, m_config.rusdAddr() ),
				m_config.busd().getPosition(bobAddr),
				m_config.rusd().getPosition(bobAddr),
				new StockToken("0x5195729466e481de3c63860034fc89efa5fbbb8f").getPosition(bobAddr) );
	}
	
	public void testTotalSupply() throws Exception {
		double i = m_config.rusd().queryTotalSupply();
		assertTrue( i > 0);
	}
}
