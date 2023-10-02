package testcase;

import fireblocks.Accounts;
import fireblocks.Fireblocks;
import fireblocks.RetVal;
import fireblocks.StockToken;

/** You only need to run this once after tokens are deployed */
public class TestSmartToken extends MyTestCase {
	static StockToken st;
	
	static {
		try {
			st = StockToken.deploy(
					"c:/work/smart-contracts/build/contracts/stocktoken.json",
					"AAA",
					"AAA",
					m_config.rusdAddr() );			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public static void main(String[] args) {
//		StockToken st = new StockToken("0x00e69bad036b0a6a3b706eb63b22ff6291ab53d8");
//		m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1);
//		Fireblocks.waitForStatus(id, "FAILED");
//	}

	public void testFailSetRusdAddress() throws Exception {
		// set RUSD address -> fail due to owner-only
		String id = st.setRusdAddress( Accounts.instance.getId("Admin1"), dead);
		Fireblocks.waitForStatus(id, "FAILED");

		// should succeed 
		id = m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1);
		Fireblocks.waitForHash(id, 60, 2000);

		// set RUSD address -> success
		id = st.setRusdAddress( Accounts.instance.getId("Owner"), TestOrder.dead);
		Fireblocks.waitForHash(id, 60, 2000);

		// should fail now that RUSD has been changed
		id = m_config.rusd().buyStockWithRusd( Cookie.wallet, 1, st, 1);
		Fireblocks.waitForStatus(id, "FAILED");

		// set RUSD back to normal
		id = st.setRusdAddress( Accounts.instance.getId("Owner"), m_config.rusdAddr() );
		Fireblocks.waitForHash(id, 60, 2000);
	}
	
	public void testTokenMint() throws Exception {
		// fail, only-RUSD
		String id = st.mint(Accounts.instance.getId("Admin1"), dead, 1); 
		Fireblocks.waitForStatus(id, "FAILED");

		// fail, only-RUSD
		id = st.mint(Accounts.instance.getId("Owner"), dead, 1); 
		Fireblocks.waitForStatus(id, "FAILED");
	}

	public void testTokenBurn() { // onlyRusdAddress {
	}
}
