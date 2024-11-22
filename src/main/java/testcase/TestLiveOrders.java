package testcase;

import tw.util.S;
import web3.StockToken;

public class TestLiveOrders extends MyTestCase {
	public void testFillBuy() throws Exception {
		StockToken stockToken = chain.getTokenByConid(265598);
		
		S.out( "pos: " + stockToken.getPosition( Cookie.wallet) );

		// buy 10
		mintRusd( Cookie.wallet, 10 * (TestOrder.curPrice + 3) + 1);
		var json = postOrderToObj( TestOrder.createOrderWithOffset( "BUY", 10, 3) );  // try again w/ autofill off
		assert200();
		var uid = json.getString( "id");
		
		// first this order should show up in the 'orders' list
		var ord1 = getAllLiveOrders(Cookie.wallet)
				.getArray( "orders");
		assertTrue( ord1.has( "id", uid) );
		
		// wait for it to show up in the 'messages' list
		waitFor( 30, () -> {
			var msgs = getAllLiveOrders(Cookie.wallet)
					.getArray( "messages");
			return msgs.has( "id", uid);
		});
		
		var ord3 = getAllLiveOrders(Cookie.wallet)
				.getArray( "orders");
		assertFalse( ord3.has( "id", uid) );
	}


}
