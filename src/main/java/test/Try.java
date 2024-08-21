package test;

import java.math.BigInteger;

import refblocks.Refblocks;
import reflection.Config;
import tw.util.S;
import web3.NodeServer;

/** Just test that you can connect to the database. */
public class Try {
	static Config c;

	static {
		try {
			c = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void cancelTransaction( String key, int nonce) {
				
	}
	
	public static void main(String[] args) throws Exception {
		// this fails; why?
		String pk = "8d97695add1e3f8dee51555eadd0258d2d07d19c6a0c0827154eb4aeed52631d";
		c.busd().transfer(pk, c.refWalletAddr(), 50).displayHash();
		
	}
}
