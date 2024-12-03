package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;

public class FillTheGap {
	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			String name = s.getString( "enter chain name: (e.g. Polygon)");

			Chain chain = new Chains().readOne( name, false);
			chain.blocks().showAllNonces( chain.params().admin1Addr() );

			int num = s.getInt( "enter number of transactions)");

			for (int i = 0; i < num; i++) {
				chain.blocks().transfer( chain.params().admin1Key(), NodeInstance.nullAddr, 0)
					.waitForReceipt();
			}
		}
	}
}
