package testcase.web3;

import fireblocks.Accounts;
import fireblocks.FbStockToken;
import reflection.Config.Web3Type;
import testcase.Cookie;
import testcase.MyTestCase;
import web3.StockToken;

/** You only need to run this once after tokens are deployed */
public class TestStockToken extends MyTestCase {

	/** fireblocks only */
	public void testFailSetRusdAddress() throws Exception {
		StockToken st = stocks.getAnyStockToken();
		FbStockToken fbSt = new FbStockToken( st);

		if (m_config.web3Type() == Web3Type.Fireblocks) {
		
			// set RUSD address -> fail due to owner-only
			shouldFail( () -> fbSt.setRusdAddress( Accounts.instance.getId("Admin1"), dead)
					.waitForHash() );

			// set RUSD address to dead; succeed
			fbSt.setRusdAddress( Accounts.instance.getId("Owner"), dead)
				.waitForHash();

			// buy stock should fail now that RUSD has been changed
			shouldFail( () -> m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1)
					.waitForCompleted() );  // note that waitForHash() succeeds!!!

			// set RUSD back to normal
			fbSt.setRusdAddress( Accounts.instance.getId("Owner"), m_config.rusd().address() )
				.waitForHash();
		}
	}
	
	public void testTokenMint() throws Exception {
		StockToken st = stocks.getAnyStockToken();
		FbStockToken fbSt = new FbStockToken( st);
		
		// should fail only owner
		shouldFail( () -> fbSt.mint(Accounts.instance.getId("Admin1"), dead, 1)
				.waitForHash() );
	}
}
