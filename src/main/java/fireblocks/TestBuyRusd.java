package fireblocks;

import reflection.Config;
import tw.util.S;

public class TestBuyRusd {
	static Accounts accounts = Accounts.instance;
	
	public static void main(String[] args) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");
		
		String file = "C:/Work/Smart-contracts/build/contracts/RUSD.json";
		
		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");
		
		Busd busd = config.busd();
		
		Rusd rusd = config.rusd();
		
//		rusd.deploy(
//				file,
//				accounts.getAddress( "RefWallet"),				
//				accounts.getAddress("Admin1") );
//		
//		// mint BUSD for user Bob
//		busd.mint(
//				accounts.getId( "Admin1"),
//				accounts.getAddress("Bob"),
//				2);
//
//		// user to approve buying with BUSD; you must wait for this
//		busd.approve(
//				accounts.getId( "Bob"),
//				rusd.address(),
//				2).waitForHash();
		
		S.out( rusd.buyRusd(
				accounts.getAddress("Bob"),
				busd,
				1).id() );
		
//		rusd.buyRusd(
//				accounts.getAddress("Bob"),
//				busd,
//				1);
//		
//		rusd.sellRusd(
//				accounts.getAddress("Bob"),
//				busd,
//				2);
		
	}
}
