package test;

import chain.Chain;
import chain.Chains;
import common.Util;

public class ClearWalletsOfRusd {
	public static void main(String[] args) throws Exception {
		Chain chain = Chains.readOne( "Polygon", true);
		var map = chain.rusd().getAllBalances();  // map address -> balance
		Util.forEach( map, (wallet,bal) -> {
			if (bal >= .005) {
				chain.rusd().burnRusd(wallet, bal, chain.getAnyStockToken() );
			}
		});
	}
}
