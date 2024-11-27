package test;

import reflection.Config.MultiChainConfig;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		MultiChainConfig c1 = new MultiChainConfig();
		c1.readFromSpreadsheet("Prod-config");
		var node = c1.chains().polygon().node();
		S.out( node.getBlockDateTime( node.getBlockNumber() ) );

		
	}
}
