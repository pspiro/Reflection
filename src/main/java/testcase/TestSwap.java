package testcase;

import java.util.Iterator;

import common.Util;
import positions.Wallet;
import reflection.Stock;
import web3.StockToken;

public class TestSwap extends MyTestCase {
	
	public void testSwap() throws Exception {
		Iterator<Stock> set = stocks.stockSet().iterator();
		
		// get two stock tokens
		StockToken stock1 = set.next().getToken();
		StockToken stock2 = set.next().getToken();
		
		// mint one, then swap it for another
		String wallet = Util.createFakeAddress();
		m_config.rusd().mintStockToken( wallet, stock1, 8).waitForHash();   // succeeds
		waitForBalance( wallet, stock1.address(), 8, false);
		
		m_config.rusd().swap( wallet, stock1, stock2, 3, 4).waitForHash();
		
		waitFor( 60, () -> 
			Wallet.getBalance(wallet, stock1.address() ) == 5 &&
			Wallet.getBalance(wallet, stock2.address() ) == 4
		);
	}
	
    	
}
