package web3;

import tw.util.S;

public abstract class RetVal {
	/** This blocks for up to 63 seconds */
	public abstract String waitForHash() throws Exception;

	public abstract void waitForCompleted() throws Exception;

	public void displayHash() throws Exception {
		S.out( waitForHash() );
	}
}
