package testcase;

import static testcase.TestOrder.curPrice;

import reflection.RefCode;
import tw.util.S;

public class TestUserTokMgr extends MyTestCase {
	
	public void test() throws Exception {
		Cookie.setNewFakeAddress( true);
		//Cookie.setWalletAddr("0xa65dF27175EE0C3f9c2bf781EaF85726128220D8");

		// mint enough RUSD for two orders
		double buyPrice = curPrice * 1.1;
		double amt = buyPrice * 2 + 5;
		S.out( "Minting %s RUSD", amt);
		m_config.rusd().mintRusd( Cookie.wallet, amt, stocks.getAnyStockToken() )  // I don't think this is necessary but I saw it fail without this
			.waitForCompleted();
		waitForRusdBalance(Cookie.wallet, amt, false);

		// first order - should pass
		postOrderToObj( TestOrder.createOrder3( "BUY", 1, buyPrice, "RUSD") );
		assert200();

		// second order - should pass
		postOrderToObj( TestOrder.createOrder3( "BUY", 1, buyPrice, "RUSD") );
		assert200();
		
		// third order - should fail
		postOrderToObj( TestOrder.createOrder3( "BUY", 1, buyPrice, "RUSD") );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INSUFFICIENT_STABLECOIN, cli.getRefCode() );

		// wait for apple balance
		waitForBalance(Cookie.wallet, "0xda0984159f86dfa0fd82177a7d4ada226b7b0910", 2, false);

		// first order - should pass
		double sellPrice = curPrice * .9;
		postOrderToObj( TestOrder.createOrder3( "SELL", 1, sellPrice, "RUSD") );
		assert200();

		// second order - should pass
		postOrderToObj( TestOrder.createOrder3( "SELL", 1, sellPrice, "RUSD") );
		assert200();
		
		// third order - should fail
		postOrderToObj( TestOrder.createOrder3( "SELL", 1, sellPrice, "RUSD") );
		assertEquals( 400, cli.getResponseCode() );
		assertEquals( RefCode.INSUFFICIENT_STOCK_TOKEN, cli.getRefCode() );
		
		// you might want to create log entries for the updates to the UserTokMgr
		
		// TEST SELL
	}
}
