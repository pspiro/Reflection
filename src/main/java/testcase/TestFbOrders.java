package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import redis.ConfigBase.Web3Type;
import reflection.RefCode;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;
import web3.Busd;
import web3.Matic;
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
			bobKey = m_config.web3Type() == Web3Type.Fireblocks 
					? "bob" 
					: "b138aae3e4700252c20dc7f9548a0982db73c70e10db535fda13c11ea26077fd";
			
			bobAddr = Matic.getAddress( bobKey);

			GTable tab = new GTable( NewSheet.Reflection, m_config.symbolsTab(), "TokenSymbol", "TokenAddress");
			stock = new StockToken( tab.get( "GOOG") );
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
		
		cli().get("/api/mywallet/" + bobAddr);
		double bal = cli.readJsonObject()
				.getArray("tokens")
				.find( "name", busd.name() )
				.getDouble("balance");
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
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting non-approval");

		// mint BUSD for user Bob
		// mint BUSD for user Bob
		S.out( "**minting 2000");
		m_config.mintBusd( bobAddr, 2000)  // I don't think this is necessary but I saw it fail without this
			.waitForHash();
		
		// approve too little
		S.out( "**approving .49");
		busd.approve(
				bobKey,
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

	/** There must be a valid profile for Bob for this to work */
	public void testFillWithFb() throws Exception {  // always fails the second time!!!
		S.out( "-----testFillWithFb");
		Cookie.setWalletAddr(bobAddr);
		showAmounts("starting amounts");

		// make sure we have a valid user profile  (updates profile for Cookie.wallet
		//cli().post("/api/update-profile", TestProfile.createValidProfile().toString() );
		// this doesn't work because we can't update the email
		
		// mint BUSD for user Bob
		S.out( "**minting 2000");
		busd.mint( bobAddr, 2000).waitForHash();
		waitForBalance( bobAddr, m_config.busd().address(), 2000, false);
		
		// user to approve buying with BUSD; you must wait for this
		S.out( "**approving 20000");
		busd.approve(
				bobKey,
				rusd.address(),
				2000).waitForHash();
		waitFor( 30, () -> busd.getAllowance( bobAddr, rusd.address()) > 1999);
		showAmounts("updated amounts");

		JsonObject obj = TestOrder.createOrder( "BUY", 1, 3);
		obj.remove("noFireblocks");
		
		S.out( "**Submitting: " + obj);
		JsonObject map = postOrderToObj(obj);
		assert200();
		
		String uid = map.getString("id");  // 5-digit code
		assertTrue( S.isNotNull(uid) );
		S.out( "Submitted order with uid %s", uid);

		// wait for order to complete
		waitFor( 30000, () -> {
			JsonObject liveOrders = getAllLiveOrders(bobAddr);
			S.out( liveOrders);

			JsonObject msg = liveOrders.getArray("messages").find("id", uid);
			JsonObject order = liveOrders.getArray("orders").find("id", uid);
			
			if (msg != null) {
				S.out("Completed: " + msg);
				assertEquals( "message", msg.getString("type"));
				startsWith( "Bought", msg.getString("text") );
				return true;
			}
			assertTrue(order != null);
			return false;
		});
		
		// fetch most recent transaction from database
		JsonArray ar = m_config.sqlQuery( conn -> conn.queryToJson(
				"select * from transactions order by created_at desc limit 1") );
		assertTrue( ar.size() > 0);
		
		JsonObject rec = ar.get(0);
		S.out(rec);
		assertEquals( "COMPLETED", rec.getString("status") );
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
