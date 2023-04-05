package fireblocks;

import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

/** Tests all BUSD and StockToken features */
public class Test {
	// it seems that you have to wait or call w/ the same Admin
	// if you need the first transaction to finish because
	// Fireblocks checks and thinks it will fail if the first
	// one is not done yet
	
	
	static Accounts accounts = Accounts.instance;
	//static String userAddr = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String userAddr = "0x1cB79caf8c86f04bD31C4AD1f43A5ba17d61BD35";	

	public static void main(String[] args) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");
		
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");

		run( config.newBusd(), config.newRusd() );
	}
	
	static void run(Busd busd, Rusd rusd) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");
		
		GTable tab = new GTable( NewSheet.Reflection, "Test-symbols", "ContractSymbol", "TokenAddress");
		StockToken stock = new StockToken( tab.get( "GOOG") );
		
		
		// ----- Bob -----------------------------
		
		// mint BUSD for user Bob
		busd.mint(
				accounts.getId( "Admin1"),
				accounts.getAddress("Bob"),
				1);

		// user to approve buying with BUSD; you must wait for this
		busd.approve(
				accounts.getId( "Bob"),
				rusd.address(),
				1).waitForHash();
		
		// let Bob buy 1 stock with 1 BUSD (now RefWallet has 1 BUSD which is needed when user wants to sell their RUSD) 
		rusd.buyStock(
				accounts.getAddress("Bob"),
				busd,
				1,
				stock,
				1);

		//S.input("Check balances, should be 1 BUSD in RefWallet");
		
		// ----- 0x450e -----------------------------
		
		// let user buy stock with zero RUSD
		rusd.buyStockWithRusd(
				userAddr,
				0, 
				stock, 
				1);
		
		// sell stock for 1 RUSD
		rusd.sellStockForRusd(
				userAddr,
				1,
				stock,
				1);
		
		// buy stock for 1 RUSD
		rusd.buyStockWithRusd(
				userAddr,
				1, 
				stock, 
				1);
				
		// sell stock for 1 RUSD
		rusd.sellStockForRusd(
				userAddr,
				1,
				stock,
				1);
		
		// redeem (sell) RUSD for BUSD
		rusd.sellRusd(
				userAddr,
				busd,
				1);
	}

}
