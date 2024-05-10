package testcase.web3;

/** You only need to run this once after tokens are deployed */
/*public class TestStockToken extends MyTestCase {
	static StockToken st;
	
	static {
		readStocks();
		try {
			st = stocks.getAnyStockToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testFailSetRusdAddress() throws Exception {
		
//		// set RUSD address -> fail due to owner-only
//		st.setRusdAddress( Accounts.instance.getId("Admin1"), dead)
//			.waitForStatus("FAILED");

		// should succeed 
		m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1)
				.waitForHash();
		
		// fail burn, only-RUSD
//		st.burn(Accounts.instance.getId("Admin1"), dead, 1)
//				.waitForStatus("FAILED");
//
//		// fail burn, only-RUSD
//		st.burn(Accounts.instance.getId("Owner"), dead, 1) 
//				.waitForStatus("FAILED");

//		// set RUSD address -> success
//		st.setRusdAddress( Accounts.instance.getId("Owner"), TestOrder.dead)
//				.waitForHash();

		// should fail now that RUSD has been changed
		m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1)
				.waitForStatus("FAILED");

//		// set RUSD back to normal
//		st.setRusdAddress( Accounts.instance.getId("Owner"), m_config.rusdAddr() )
//				.waitForHash();
	}
	
	public void testTokenMint() throws Exception {
		try {
			st.mint(Accounts.instance.getId("Admin1"), dead, 1).wait
//				.waitForStatus("FAILED");
		}
		catch( Exception e) {
			// okay
		}
	}
//
//		// fail, only-RUSD
//		st.mint(Accounts.instance.getId("Owner"), dead, 1) 
//				.waitForStatus("FAILED");
	}

	public void testFailTokenBurn() throws Exception {
		
	}
}
*/