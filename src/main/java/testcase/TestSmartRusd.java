package testcase;

import static fireblocks.Accounts.instance;
import fireblocks.Fireblocks;
import fireblocks.RetVal;
import fireblocks.Rusd;
import fireblocks.StockToken;

public class TestSmartRusd extends MyTestCase {
	public void testAORA() throws Exception {
		String[] paramTypes = { "address", "uint256" };
		Object[] params = { dead, 1 };

		// should fail, only-owner
		Fireblocks.call2(
				instance.getId( "Admin1"),
				m_config.rusdAddr(),
				Rusd.addOrRemoveKeccak, 
				paramTypes, 
				params, 
				"RUSD add admin")
			.waitForStatus("FAILED");
	}
	
	public void testAdmin() throws Exception {
		// deploy ST and RUSD

		instance.setAdmins("Admin1");
		
		Rusd rusd = new Rusd("", 6);
		rusd.deploy("c:/work/smart-contracts/build/contracts/rusd.json", dead, instance.getAddress( "Admin1") ); 
		StockToken st = StockToken.deploy(
				"c:/work/smart-contracts/build/contracts/stocktoken.json",
				"AAA",
				"AAA",
				rusd.address() );

		// mint RUSD
		new RetVal(rusd.mint(dead, 10.0, st))
			.waitForHash();
		
		// buy a stock token - succeed
		new RetVal(rusd.buyStockWithRusd(dead, 1, st, 1))
			.waitForHash();
		
		// remove admin
		rusd.addOrRemoveAdmin(dead, false);
		
		// buy a stock token - fail
		new RetVal(rusd.buyStockWithRusd(dead, 1, st, 1))
			.waitForStatus("FAILED");
	}
}
