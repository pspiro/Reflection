package fireblocks;

import tw.util.S;
import web3.Matic;
import web3.RetVal;

public class FbMatic extends Matic {

	@Override public RetVal transfer(String fromAcct, String to, double amt) throws Exception {
		return Fireblocks.transfer( 
				Accounts.instance.getId( fromAcct),
				to,
				Fireblocks.platformBase,
				amt,  // do not convert
				"transfer native token");
	}

	@Override public String getAddress(String accountName) throws Exception {
		return Accounts.instance.getAddress( accountName); 
	}
	
	public void createSystemWallets() throws Exception {
		createWallet( "Owner");
		createWallet( "RefWallet");
		createWallet( "Admin1");
//		create( "Admin2");
	}

	private static void createWallet(String account) throws Exception {
		Fireblocks.createWallet( Accounts.instance.getId(account), Fireblocks.platformBase);
		S.out( "created wallet " + Accounts.instance.getAddress(account));
	}
}
