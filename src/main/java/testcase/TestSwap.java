package testcase;

import common.Util;
import web3.StockToken;

public class TestSwap extends MyTestCase {
	
	public void testSwap() throws Exception {
		var iter = chain.getTokens().iterator();
		
		// get two stock tokens
		StockToken stock1 = iter.next();
		StockToken stock2 = iter.next();
		
		// mint one, then swap it for another
		String wallet = Util.createFakeAddress();
		m_config.rusd().mintStockToken( wallet, stock1, 8).waitForReceipt();   // succeeds
		waitForBalance( wallet, stock1.address(), 8, false);
		
		m_config.rusd().swap( wallet, stock1, stock2, 3, 4).waitForReceipt();
		
		waitFor( 60, () -> 
			stock1.getPosition( wallet) == 5 &&
			stock2.getPosition( wallet) == 4
		);
	}
	
    	
}
