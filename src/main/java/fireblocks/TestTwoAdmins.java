package fireblocks;

import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;

public class TestTwoAdmins {
	static Busd busd;
	static Rusd rusd;
	static StockToken stock;
	
	public static void main(String[] args) throws Exception {
		Accounts.instance.setAdmins( "Admin1,Admin2");
		
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");

		GTable tab = new GTable( NewSheet.Reflection, "Test-symbols", "ContractSymbol", "TokenAddress");
		stock = new StockToken( tab.get( "GOOG") );

		rusd = config.newRusd();
		busd = config.newBusd();
		
		prepare("Bob");
		prepare("Sam");

		buy("Bob");
		buy("Sam");
		buy("Bob");
		buy("Sam");
	}
	
	static void buy(String name) throws Exception {
		rusd.buyStock(
				Accounts.instance.getAddress(name),
				busd,
				1,
				stock,
				1);
	}

	/** Mint BUSD for user and approve RUSD for trading it */
	static void prepare(String name) throws Exception {
		String addr = Accounts.instance.getAddress(name);
		
		busd.mint(
				Accounts.instance.getAdminAccountId(addr),
				addr,
				2);

		// user to approve buying with BUSD; you must wait for this
		busd.approve(
				Accounts.instance.getId(name),
				rusd.address(),
				2).waitForHash();
	}
}

