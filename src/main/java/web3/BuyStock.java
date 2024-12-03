package web3;

import reflection.Config.MultiChainConfig;

/** Just test that you can connect to the database. */
public class BuyStock {
	public static void main(String[] args) throws Exception {
		MultiChainConfig c1 = new MultiChainConfig();
		c1.readFromSpreadsheet("prod-config");
		
		var poly = c1.chains().polygon();
		poly.blocks().showAllNonces( poly.params().admin1Addr() );
				
		poly.rusd().buyStockWithRusd(
				NodeInstance.prod, 
				1, 
				c1.chains().polygon().getAnyStockToken(), 
				1).waitForReceipt();
		
	}
}
