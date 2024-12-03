package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;
import common.Util;
import tw.util.S;

/** This actually works as of 8/19/24 on pulsechain */
public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		Chain poly = new Chains().readOne( "Polygon", false);
		
		String wallet = poly.params().admin1Addr();  // wallet that is stuck
		
		// show current nonces
		poly.blocks().showAllNonces( poly.params().admin1Addr() );

		// show nonces of stuck transactions
		poly.node().showTrans( wallet);

		// you have to cancel the lowest nonce first, but then the others will go through
		// you might have to replace the others, not wait for a receipt, then cancel the lowest
						
		try (MyScanner s = new MyScanner() ) {
			while( true) {
				String str = s.input( "enter nonce to cancel: ");
				if (S.isNull( str) ) {
					break;
				}
				
				poly.blocks().cancelStuckTransaction( poly.params().admin1Key(), Integer.parseInt( str) );
			}
		}

	}
	
	//static void fillTheGap() {
}
