package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;

public class ShowNonces {

	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			String chain = s.getString( "enter chain name: (e.g. Polygon)");
			Chain poly = Chains.readOne( chain, false);
			poly.blocks().showAllNonces( poly.params().admin1Addr() );
		}
	}
}
