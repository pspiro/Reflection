package web3;

/** An abstraction of the native token on whatever chain we are using */
public abstract class Matic {
	
	/** This blocks until we have the receipt, no need to call waitForHash() */
	public abstract RetVal transfer(String senderKey, String to, double amt) throws Exception;
	public abstract String getAddress(String key) throws Exception;
	public abstract void createSystemWallets() throws Exception;
}
