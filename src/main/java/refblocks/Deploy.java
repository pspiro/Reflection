package refblocks;


import common.Util;
import redis.ConfigBase.Web3Type;
import reflection.Config;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import web3.Busd;
import web3.Rusd;

public class Deploy {
	// it seems that you have to wait or call w/ the same Admin
	// if you need the first transaction to finish because
	// Fireblocks checks and thinks it will fail if the first
	// one is not done yet
	
	// deploy RUSD and all stock tokens
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		Util.require(config.web3Type() == Web3Type.Refblocks, "Turn on Fireblocks");
		
		Rusd rusd = config.rusd();
		Busd busd = config.busd();
		
		S.out( "Deploying system");

		// deploy BUSD? (for testing only)
		if ("deploy".equals( busd.address() ) ) {
			String address = RbBusdCore.deploy( config.ownerKey() );
			config.setBusdAddress( address);  // update spreadsheet with deployed address
			busd = config.busd();
		}
		else {
			Util.require( Util.isValidAddress( busd.address() ), "BUSD must be valid or set to 'deploy'");
		}
		
		// deploy RUSD (if set to "deploy")
		if ("deploy".equalsIgnoreCase( rusd.address() ) ) {
			String address = RbRusdCore.deploy( config.ownerKey(), config.refWalletAddr(), config.admin1Addr() );
			config.setRusdAddress( address);  // update spreadsheet with deployed address
			rusd = config.rusd();

			// let RefWallet approve RUSD to transfer BUSD
			busd.approve( 
					rusd.address(), // approving
					1000000000); // $1B

			// add a second admin
//			rusd.addOrRemoveAdmin(
//					instance.getAddress( "Admin2"), 
//					true);
		}
		else {
			Util.require( Util.isValidAddress( rusd.address() ), "RUSD must be valid or set to 'deploy'");
		}
		
		// deploy stock tokens where address is set to deploy (should be inactive to prevent errors in RefAPI)
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.symbolsTab() ).fetchRows(false) ) {
			if (row.getString( "Token Address").equalsIgnoreCase("deploy") ) {
				// deploy stock token
				String address = RbStockTokenCore.deploy(
						config.ownerKey(),
						row.getString( "Token Name"),  // wrong, this should get pulled from master symbols tab
						row.getString( "Token Symbol"),
						rusd.address()
				);
				
				// update row on Symbols tab with new stock token address
				row.setValue( "Token Address", address);
				row.update();
			}
		}
		
		//Test.run(config, busd, rusd);
	}
// test build	
}
