package refblocks;

import org.web3j.crypto.Credentials;

import common.Util;
import web3.Matic;
import web3.RetVal;

public class RbMatic extends Matic {

	@Override public RetVal transfer(String senderKey, String to, double amt) throws Exception {
		return Refblocks.transfer( senderKey, to, amt);
	}

	/** this is very fast */
	@Override public String getAddress(String key) throws Exception {
		Util.require( Util.isValidKey(key), "not a valid private key");
		return Credentials.create( key ).getAddress();
	}
}
