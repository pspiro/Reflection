package test;

import reflection.Config.MultiChainConfig;
import tw.util.S;
import web3.NodeInstance;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		MultiChainConfig c1 = new MultiChainConfig();
		c1.readFromSpreadsheet("prod-config");
		
		c1.chains().polygon().rusd().buyStockWithRusd(
				NodeInstance.prod, 
				1, 
				c1.chains().polygon().getAnyStockToken(), 
				1).waitForReceipt();
		
	}
}
