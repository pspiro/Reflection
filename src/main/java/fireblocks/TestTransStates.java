package fireblocks;

import org.json.simple.JsonObject;

import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.OStream;
import tw.util.S;

public class TestTransStates {
	static Accounts accounts = Accounts.instance;

	public static void main(String[] args) throws Exception {
		try (OStream os = new OStream("c:/temp/file.t") ) {
		
		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");
		
		Busd busd = config.busd();
		Rusd rusd = config.rusd();
		
		accounts.setAdmins( "Admin1,Admin2");

		GTable tab = new GTable( NewSheet.Reflection, config.symbolsTab(), "ContractSymbol", "TokenAddress");
		StockToken stock = new StockToken( tab.get( "GOOG") );
		S.out( "Buying/sell GOOG stock token with address %s", stock.address() );
		
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
		String id = rusd.buyStock(
				accounts.getAddress("Bob"),
				busd,
				1,
				stock,
				100);
		
		for (int i = 0; ; i++) {
			if (i > 0) S.sleep(1000);
			JsonObject trans = Transactions.getTransaction( id);
			S.out( "%s %s", trans.getString("status"), trans);
			os.writeln( "%s %s", trans.getString("status"), trans);
		}
		}
	}
	
}