package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;
import common.Util;
import tw.util.S;

/** This actually works as of 8/19/24 on pulsechain */
public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			String chain = s.getString( "enter chain name: (e.g. Polygon)");
		
		Chain poly = new Chains().readOne( chain, false);
		
		String wallet = poly.params().admin1Addr();  // wallet that is stuck
		
		// show current nonces
		poly.blocks().showAllNonces( poly.params().admin1Addr() );

		// show nonces of stuck transactions
		poly.node().showTrans( wallet);

		// you have to cancel the lowest nonce first, but then the others will go through
		// you might have to replace the others, not wait for a receipt, then cancel the lowest
						
			while( true) {
				String str = s.getString( "enter nonce to cancel: ");
				if (S.isNull( str) ) {
					break;
				}
				
				poly.blocks().cancelStuckTransaction( poly.params().admin1Key(), Integer.parseInt( str) );
			}
		}

	}
	
	//static void fillTheGap() {
}

/* There was a very annoying and hard to fix issue. When the admin1 wallet
ran out of gas, many transactions were submitted with increasing nonces.
They all failed, but they stayed in the 'queued' transactions list. But
only in Asia; they did not show up in the US. And only in the main RPC 
node.

These transactions could not be canceled because there was a nonce gap.

The solution was to switch to another RPC node (Moralis). I suppose each
node keeps it's own queue. Another solution which may have worked would
be to submit dummy transactions to fill the nonce gap, and then to keep
going to cancel the queued transactions.

I must wonder if I can ever switch back to the main RPC node.
*/