package web3;

/** An abstraction of the native token on whatever chain we are using */
public abstract class Matic {
	
	public abstract void send(String privateKey, String to, double amt) throws Exception;

}
