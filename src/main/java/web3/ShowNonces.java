package web3;

import chain.Chain;
import chain.Chains;

public class ShowNonces {

	public static void main(String[] args) throws Exception {
		Chain poly = new Chains().readOne( "Polygon", false);
		poly.blocks().showAllNonces( poly.params().admin1Addr() );
	}
}
