package fireblocks;

import static fireblocks.Accounts.instance;

import common.Util;
import reflection.Config;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class Deploy {
	// it seems that you have to wait or call w/ the same Admin
	// if you need the first transaction to finish because
	// Fireblocks checks and thinks it will fail if the first
	// one is not done yet
	
	// deploy RUSD and all stock tokens
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Prod-config");
		Util.require(config.useFireblocks(), "Turn on Fireblocks");
		
		Rusd rusd = config.rusd();
		Busd busd = config.busd();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		if ("deploy".equals( busd.address() ) ) {
			busd.deploy("c:/work/smart-contracts/build/contracts/busd.json");
			config.setBusdAddress( busd.address() );  // update spreadsheet with deployed address
		}
		else {
			Util.require( Util.isValidAddress( busd.address() ), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equals( rusd.address() ) ) {
			rusd.deploy( 
					"c:/work/smart-contracts/build/contracts/rusd.json",
					instance.getAddress( "RefWallet"),
					instance.getAddress( "Admin1")	);
			config.setRusdAddress( rusd.address() );  // update spreadsheet with deployed address

			// let RefWallet approve RUSD to transfer BUSD
			busd.approve( 
					instance.getId( "RefWallet"), // called by
					rusd.address(), // approving
					1000000000); // $1B

			// add a second admin
			rusd.addOrRemoveAdmin(
					instance.getAddress( "Admin2"), 
					true);
		}
		else {
			Util.require( Util.isValidAddress( rusd.address() ), "RUSD must be valid or set to 'deploy'");
		}
		
		// deploy stock tokens that are active and have an empty token address
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.symbolsTab() ).fetchRows(false) ) {
			if ("Y".equals( row.getString( "Active") ) && S.isNull( row.getString( "Token Address") ) ) {
				// deploy stock token
				StockToken token = StockToken.deploy( 
						"c:/work/smart-contracts/build/contracts/stocktoken.json",						
						row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
						row.getString( "Token Symbol"),
						rusd.address()
				);
				
				// update row on Symbols tab with new stock token address
				row.setValue( "Token Address", token.address() );
				row.update();
			}
		}
		
		//Test.run(config, busd, rusd);
	}
	
}
