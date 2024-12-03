package test;

import reflection.Config.MultiChainConfig;
import tw.util.S;
import web3.NodeInstance;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		MultiChainConfig c1 = new MultiChainConfig();
		c1.readFromSpreadsheet("prod-config");
		
		var poly = c1.chains().polygon();
		
		S.out( "nonce=%s", poly.node().getNonce( poly.params().admin1Addr() ) );
		S.out( "noncePending=%s", poly.node().getNoncePending( poly.params().admin1Addr() ) );
		
		poly.rusd().buyStockWithRusd(
				NodeInstance.prod, 
				1, 
				c1.chains().polygon().getAnyStockToken(), 
				1).waitForReceipt();
		
	}
}
