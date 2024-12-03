package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;

/** Just test that you can connect to the database. */
public class BuyStock {
	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			String name = s.getString( "enter chain name: (e.g. Polygon)");
			Chain chain = new Chains().readOne( name, true);

			chain.blocks().showAllNonces( chain.params().admin1Addr() );

			chain.rusd().buyStockWithRusd(
					NodeInstance.prod, 
					1, 
					chain.getAnyStockToken(), 
					1).waitForReceipt();
		}
	}
}
