package testcase;

import fireblocks.Busd;
import fireblocks.Rusd;
import fireblocks.StockToken;
import json.MyJsonArray;
import json.MyJsonObject;
import reflection.RefCode;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class TestFbOrders extends MyTestCase {
	// test all possible orders and a success of buy w/ BUSD, buy w/ RUSD, and sell
	
	static String userAddr = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static GTable tab;
	static StockToken stock;
	static Busd busd;
	static Rusd rusd;

	static {
		try {
			tab = new GTable( NewSheet.Reflection, m_config.symbolsTab(), "ContractSymbol", "TokenAddress");
			stock = new StockToken( tab.get( "GOOG") );
			busd = m_config.busd();
			rusd = m_config.rusd();
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	public void testInsufficientFundsBuy() throws Exception {
		cli().get("/api/mywallet/" + Cookie.wallet);
		double bal = cli.readMyJsonObject().getAr("tokens").find( "name", "USDC").getDouble("balance");
		double maxQty = bal / (TestOrder.curPrice+3);  // this is the max we could buy w/ the current balance
		
		MyJsonObject obj = TestOrder.createOrder( "BUY", maxQty + 1, 3);
		obj.remove("noFireblocks");
		
		MyJsonObject map = postOrderToObj(obj);
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( RefCode.OK, cli.getCode() );
		
		while(true) {
			MyJsonObject ret = getLiveMessage2(map.getString("id"));
			if (ret != null) {
				S.out(ret);
				assertEquals( "error", ret.getString("type"));
				startsWith( "The stablecoin balance", ret.getString("text") );
				break;
			}
			S.sleep(1000);
		}
	}

	public void testNonApproval() throws Exception {
		// mint BUSD for user Bob
		busd.mint(
				accounts.getId( "Admin1"),
				accounts.getAddress("Bob"),
				200);
		
		// approve too little
		busd.approve(
				accounts.getId( "Bob"),
				rusd.address(),
				1).waitForHash();
		
		MyJsonObject obj = TestOrder.createOrder( "BUY", 1, 3);
		obj.remove("noFireblocks");
		
		MyJsonObject map = postOrderToObj(obj);
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( RefCode.OK, cli.getCode() );
		
		while(true) {
			MyJsonObject ret = getLiveMessage2(map.getString("id"));
			if (ret != null) {
				S.out(ret);
				assertEquals( "error", ret.getString("type"));
				startsWith( "The approved amount", ret.getString("text") );
				break;
			}
			S.sleep(1000);
		}
	}

	public void testFillWithFb() throws Exception {
		// mint BUSD for user Bob
		busd.mint(
				accounts.getId( "Admin1"),
				accounts.getAddress("Bob"),
				200);
		
		// user to approve buying with BUSD; you must wait for this
		busd.approve(
				accounts.getId( "Bob"),
				rusd.address(),
				200).waitForHash();
		
		String address = accounts.getAddress("Bob");
		String cookie = Cookie.signIn(address);

		//double approvedAmt = m_config.busd().getAllowance( m_walletAddr, m_config.rusdAddr() );
		
		MyJsonObject obj = TestOrder.createOrder( "BUY", 1, 3);
		obj.remove("noFireblocks");
		obj.put("wallet_public_key", address);
		obj.put("cookie", cookie);
		
		MyJsonObject map = postOrderToObj(obj);
		assertEquals( 200, cli.getResponseCode() );
		assertEquals( RefCode.OK, cli.getCode() );
		
		String uid = map.getString("id");
		assertTrue( S.isNotNull(uid) );
		S.out( "Submitted order with uid %s", uid);
		
		while(true) {
			MyJsonObject liveOrders = getLiveOrders(address);
			S.out( liveOrders);

			MyJsonObject msg = liveOrders.getAr("messages").find("id", uid);
			MyJsonObject order = liveOrders.getAr("orders").find("id", uid);
			
			if (msg != null) {
				S.out("filled: " + msg);
				assertEquals( "message", msg.getString("type"));
				startsWith( "Bought", msg.getString("text") );
				break;
			}
			
			assertTrue(order != null);
			S.out(order);

			S.sleep(1000);
		}
		
	}

}
