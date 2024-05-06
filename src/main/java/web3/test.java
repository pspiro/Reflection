package web3;

import refblocks.RbBusdCore;
import refblocks.RbRusdCore;
import reflection.Config;
import reflection.Stocks;

public class test {
	public void test() throws Exception {
		String userWallet = "0x76274e9a0F0bc4EB9389e013bD00b2c4303cDd37"; // AccountB at home
		Config config = Config.ask( "Dt");
		
		// deploy rusd
		String rusdAddr = RbRusdCore.deploy( 
				config.ownerKey(), 
				config.refWalletAddr(), 
				config.admin1Addr() 
				);
		
		Stocks stocks = config.readStocks();

		// mint rusd into user wallet
		Rusd rusd = config.rusd();
		rusd.mintRusd( userWallet, 100, stocks.getAnyStockToken() );

		String busdAddr = RbBusdCore.deploy( config.ownerKey() );
		
		Busd busd = config.busd();
		busd.mint( config.refWalletAddr(), 100);
		
		// sell rusd
		rusd.sellRusd( userWallet, busd, 75);
		
		// check balances
	}
}
// this is no good, you have to have the addresses already in the config
// so deploy the system, then test it