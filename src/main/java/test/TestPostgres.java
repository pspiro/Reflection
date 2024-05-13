package test;

import org.web3j.crypto.Credentials;

import common.Util;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		//Config config = Config.ask("Dt");
		
		String key = Util.createPrivateKey();
		
		for (int i = 0; i < 100; i++) {
			S.out( getAddress( key) );
		}
	}

	private static String getAddress(String key) {
		return Credentials.create( key ).getAddress();
	}
}
