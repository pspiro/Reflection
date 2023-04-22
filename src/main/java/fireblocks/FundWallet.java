package fireblocks;

import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;

/** This only works with the new RUSD and BUSD from the Test-config tab */
public class FundWallet {
	
	static Accounts accounts = Accounts.instance;
	//static String userAddr = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String userAddr = "0x1cB79caf8c86f04bD31C4AD1f43A5ba17d61BD35";	

	public static void main(String[] args) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");
		
		Config config = new Config();
//		config.readFromSpreadsheet("Test-config");
		config.readFromSpreadsheet("Desktop-config");

		run( config.newBusd(), config.rusd() );
	}
	
	static void run(Busd busd, Rusd rusd) throws Exception {
		accounts.setAdmins( "Admin1,Admin2");
		
		GTable tab = new GTable( NewSheet.Reflection, "Test-symbols", "ContractSymbol", "TokenAddress");
		StockToken stock = new StockToken( tab.get( "GOOG") );
		
		
		// ----- Bob -----------------------------
		
		// mint BUSD for user Bob
//		busd.mint(
//				accounts.getId( "Admin1"),
//				accounts.getAddress("Bob"),
//				1);

		// let user buy stock with zero RUSD
		rusd.buyStockWithRusd(
				userAddr,
				0, 
				stock, 
				500);
		
		// sell stock for 10000 RUSD
		rusd.sellStockForRusd(
				userAddr,
				1000,
				stock,
				400);
	}
}
