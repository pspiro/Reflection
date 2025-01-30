package test;

import chain.Chain;
import chain.Chains;
import common.MyScanner;
import common.Util;
import tw.util.S;

/** Transfer native token or Erc20 token */
public class Transfer {
	private static String pk;
	private static Chain chain;

	static {
		try {
			chain = Chains.readOne( "Polygon", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			pk = s.getString("Enter private key: ");
			transferBusd();
			transferNative();
		}
		
	}
	
	static void transferBusd() throws Exception {
		var amt = chain.busd().getPosition( Util.getAddress( pk) );
		S.out( "busd: " + amt);
		chain.busd().transfer( pk, chain.params().ownerAddr(), amt - .0001)
			.waitForReceipt();
	}
	
	static void transferNative() throws Exception {
		var amt = chain.node().getNativeBalance( Util.getAddress( pk) );
		S.out( "native balance: " + amt);
		chain.node().transfer( pk, "0x966454dCA56f75aB15Df54cee9033062D331e0d4", amt - .005) // leave enough for gas
			.waitForReceipt();
	}
}
