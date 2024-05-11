package web3;

import org.web3j.crypto.Credentials;

import common.Util;
import fireblocks.Accounts;
import refblocks.Refblocks;

/** An abstraction of the native token on whatever chain we are using */
public abstract class Matic {
	
	public abstract void send(String privateKey, String to, double amt) throws Exception;

	/** key could be a private key or a fireblocks account name 
	 * @throws Exception */
	public static String getAddress(String key) throws Exception {
		return Util.isValidKey(key)
				? Credentials.create( key ).getAddress()
				: Accounts.instance.getAddress( key); 
	}

}
