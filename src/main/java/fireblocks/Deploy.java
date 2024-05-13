package fireblocks;

import static fireblocks.Accounts.instance;

import common.Util;
import reflection.Config;
import reflection.Config.Web3Type;
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
		Config config = Config.ask();
		Util.require(config.web3Type() == Web3Type.Fireblocks, "Turn on Fireblocks");
		
		String rusdAddress = config.rusd().address();
		String busdAddress = config.busd().address();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		if ("deploy".equals( busdAddress) ) {
			busdAddress = FbBusd.deploy("c:/work/smart-contracts/build/contracts/busd.json");
			config.setBusdAddress( busdAddress);  // update spreadsheet with deployed address
		}
		else {
			Util.require( Util.isValidAddress( busdAddress), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusdAddress) ) {
			rusdAddress = FbRusd.deploy( 
					"c:/work/smart-contracts/build/contracts/rusd.json",
					instance.getAddress( "RefWallet"),
					instance.getAddress( "Admin1")	);
			config.setRusdAddress( rusdAddress);  // update spreadsheet with deployed address

			// let RefWallet approve RUSD to transfer BUSD
			new FbBusd( busdAddress, config.busd().decimals(), config.busd().name() )
				.approve( 
						Accounts.instance.getId( "RefWallet"),
						rusdAddress, 
						1000000000); // $1B

			// add a second admin
//			rusd.addOrRemoveAdmin(
//					instance.getAddress( "Admin2"), 
//					true);
		}
		else {
			Util.require( Util.isValidAddress( rusdAddress), "RUSD must be valid or set to 'deploy'");
		}
		
		// deploy stock tokens where address is set to deploy (should be inactive to prevent errors in RefAPI)
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.symbolsTab() ).fetchRows(false) ) {
			if (row.getString( "Token Address").equalsIgnoreCase("deploy") ) {
				// deploy stock token
				String address = FbStockToken.deploy( 
						"c:/work/smart-contracts/build/contracts/stocktoken.json",						
						row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
						row.getString( "Token Symbol"),
						rusdAddress
				);
				
				// update row on Symbols tab with new stock token address
				row.setValue( "Token Address", address);
				row.update();
			}
		}
		
	}
}
