package test;

import common.Util;
import reflection.SingleChainConfig;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		SingleChainConfig config = SingleChainConfig.ask( "dev3");
		
		String dest = Util.createFakeAddress();
		
		// let admin send money from owner to dest

		config.busd().mint( config.ownerKey(), config.ownerAddr(), 100000)
			.waitForReceipt();
		S.out( "balance: " + config.busd().getPosition(config.ownerAddr()));
		 
		// let owner approve admin to send money
		config.busd().approve( config.ownerKey(), config.admin1Addr(), 200)
			.waitForReceipt();
		
		double al = config.busd().getAllowance( config.ownerAddr(), config.admin1Addr() );
		S.out( "allowance: " + al);

		// let admin send money from owner to dest
		config.busd().transferFrom( config.admin1Key(), config.ownerAddr(), dest, 1)
			.waitForReceipt();
		
		S.out( "transfer completed");
		
		al = config.busd().getAllowance( config.ownerAddr(), config.admin1Addr() );
		S.out( "new balance: " + config.busd().getPosition(config.ownerAddr() ) );
		S.out( "new allowance: " + al);
		
	}
}
