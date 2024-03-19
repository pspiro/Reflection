package testcase;

import static testcase.TestOrder.curPrice;

import fireblocks.Rusd;
import reflection.RefCode;

public class TestUserTokMgr extends MyTestCase {
	static {
		try {
			readStocks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testUserTokMgr() throws Exception {
		//Cookie.setNewFakeAddress( true);
		Cookie.setWalletAddr("0x1791bB77cF4eBEDEC69e1744ef0c07357c194EaB");

		// mint enough RUSD for two orders
		double amt = curPrice * 1.1 * 2 + 5;
		Rusd rusd = m_config.rusd();
		rusd.mintRusd( Cookie.wallet, amt, stocks.getAnyStockToken() )  // I don't think this is necessary but I saw it fail without this
			.waitForCompleted();
		//waitForRusdBalance(Cookie.wallet, amt, false);

		// first order - should pass
//		postOrderToObj( TestOrder.createOrder2( "BUY", 1, curPrice * 1.1) );
//		assert200();
//
//		// second order - should pass
//		postOrderToObj( TestOrder.createOrder2( "BUY", 1, curPrice * 1.1) );
//		assert200();
//		
//		// third order - should fail
//		postOrderToObj( TestOrder.createOrder2( "BUY", 1, curPrice * 1.1) );
//		assertEquals( 400, cli.getResponseCode() );
//		assertEquals( RefCode.INSUFFICIENT_STABLECOIN, cli.getRefCode() );
			
		
		
		
//		JsonObject ord4 = TestOrder.createOrder( "SELL", 1, -3);
//		ord4.put( "fail", true);
//		postOrderToObj( ord4);
//
//		JsonObject ord5 = TestOrder.createOrder( "SELL", 1, -3);
//		ord5.put( "fail", true);
//		postOrderToObj( ord5);
//
//		Cookie.setWalletAddr( "0x2703161D6DD37301CEd98ff717795E14427a462B");
//		JsonObject ord2 = TestOrder.createOrder( "BUY", 1, 3);
//		ord2.put( "fail", true);
//		postOrderToObj( ord2);
//		
//		JsonObject ord3 = TestOrder.createOrder( "BUY", 1, 3);
//		ord3.put( "fail", true);
//		postOrderToObj( ord3);
		
		
		
	}
	

}
