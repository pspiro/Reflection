package testcase;

import static fireblocks.Accounts.instance;

import web3.Rusd;

/** Test smart contracts */
public class TestSmartRusd extends MyTestCase {
	public void testAORA() throws Exception {
		String[] paramTypes = { "address", "uint256" };
		Object[] params = { dead, 1 };

//		// should fail, only-owner
//		Fireblocks.call2(
//				instance.getId( "Admin1"),
//				m_config.rusdAddr(),
//				FRusd.addOrRemoveKeccak, 
//				paramTypes, 
//				params, 
//				"RUSD add admin")
//			.waitForStatus("FAILED");
	}
	
	public void testAdmin() throws Exception {
		// deploy ST and RUSD

		instance.setAdmins("Admin1");
		
		String admin = instance.getAddress( "Admin1");
		
		Rusd rusd = new Rusd("", 6);
		rusd.deploy("c:/work/smart-contracts/build/contracts/rusd.json", dead, admin); 
//		StockToken st = StockToken.deploy(
//				"AAA",
//				"AAA",
//				rusd.address() );
//
//		// mint RUSD
//		rusd.mintRusd(dead, 10.0, st)
//			.waitForCompleted();
//		
//		// buy a stock token - succeed
//		rusd.buyStockWithRusd(dead, 1, st, 1)
//			.waitForCompleted();
		
		// remove admin
//		rusd.addOrRemoveAdmin(admin, false)
//			.waitForCompleted();
		
		// buy a stock token - fail
//		rusd.buyStockWithRusd(dead, 1, st, 1)
//			.waitForStatus("FAILED");
	}
}
