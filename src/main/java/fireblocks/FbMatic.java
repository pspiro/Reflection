package fireblocks;

import common.Util;
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
		Util.require( !Util.isValidKey(accountName), "not a valid FB account name");
		return Accounts.instance.getAddress( accountName); 
	}
}
