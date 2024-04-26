package test;

import static fireblocks.Accounts.instance;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	
	
	public static void main(String[] args) throws Exception {
		Config config = Config.ask( "Prod");
		
		config.rusd().deploy( 
				"c:/work/smart-contracts/build/contracts/rusd.json",
				instance.getAddress( "RefWallet"),
				instance.getAddress( "Admin1")	);
	}
}
