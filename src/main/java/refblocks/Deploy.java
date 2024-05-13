package refblocks;


import common.Util;
import reflection.Config;
import reflection.Config.Web3Type;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class Deploy {
	
	// deploy RUSD, fake BUSD (for test system), and all stock tokens
	
	// NOTE you must have gas in the admin1, owner, and refWallet
	public static void main(String[] args) throws Exception {
		Config config = Config.ask("Dt");
		Util.require(config.web3Type() == Web3Type.Refblocks, "Turn on Refblocks");
		
		String rusdAddress = config.rusd().address();
		String busdAddress = config.busd().address();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		// note that the number of decimals is set in the .sol file before the Busd file is generaged */
		if ("deploy".equals( busdAddress) ) {
			busdAddress = RbBusd.deploy( config.ownerKey() );
			S.out( "deployed busd to " + busdAddress);
			config.setBusdAddress( busdAddress);  // update spreadsheet with deployed address
		}
		else {
			Util.require( Util.isValidAddress( busdAddress), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusdAddress) ) {
			rusdAddress = RbRusd.deploy( config.ownerKey(), config.refWalletAddr(), config.admin1Addr() );
			S.out( "deployed rusd to " + rusdAddress);
			config.setRusdAddress( rusdAddress);  // update spreadsheet with deployed address

			// let RefWallet approve RUSD to transfer BUSD; RefWallet needs gas for this
			//config.matic().transfer( config.ownerKey(), config.refWalletAddr(), .005);
			
			new RbBusd( busdAddress, config.busd().decimals(), config.busd().name() )
				.approve( config.refWalletKey(), rusdAddress, 1000000000); // $1B

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
				String address = RbStockToken.deploy(
						config.ownerKey(),
						row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
						row.getString( "Token Symbol"),
						rusdAddress
				);
				
				// update row on Symbols tab with new stock token address
				S.out( "deployed stock token to " + address);
				row.setValue( "Token Address", address);
				row.update();
			}
		}
		
		//Test.run(config, busd, rusd);
	}
// test build	
}
