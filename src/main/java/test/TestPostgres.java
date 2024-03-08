package test;

import positions.MoralisServer;
import reflection.Config;
import tw.util.S;


/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		Config.ask();
		MoralisServer.getAllTokenTransfers("0x40684d1e50c6848f0b782111de34713b763f919c", ar -> ar.print() );
		S.sleep(10000);
	}
}
