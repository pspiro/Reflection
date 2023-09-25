package test;

import org.json.simple.JsonObject;

import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.StockToken;
import fireblocks.Transactions;
import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class TestOneOrder {
	static Accounts accounts = Accounts.instance;

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		Busd busd = config.busd();

		GTable tab = new GTable( NewSheet.Reflection, config.symbolsTab(), "ContractSymbol", "TokenAddress");
		StockToken stock = new StockToken( tab.get( "GOOG") );
		
		// mint BUSD for user Bob
		busd.mint(
				accounts.getAddress("Bob"),
				1);
		
		// user to approve buying with BUSD; you must wait for this
		busd.approve(
				accounts.getId( "Bob"),
				config.rusdAddr(),
				1).waitForHash();
		
		// let Bob buy 1 stock with 1 BUSD (now RefWallet has 1 BUSD which is needed when user wants to sell their RUSD) 
		String id = config.rusd().buyStock(
				accounts.getAddress("Bob"),
				busd,
				1,
				stock,
				100);
		
		while(true) {
			JsonObject obj = Transactions.getTransaction(id);
			S.out( "%s %s %s",
					obj.getString("status"),
					obj.getString("createdAt"),
					obj.getString("lastUpdated")
				);
					//obj.getString("subStatus"
					
			S.sleep(1000);
		}
	}
}
