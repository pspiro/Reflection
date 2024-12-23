package test;

import chain.Chain;
import chain.Chains;
import http.MyClient;
import reflection.Config.MultiChainConfig;
import tw.util.S;

/** figure out who should be redeemed and how much */
public class Redeem {
	MultiChainConfig config;

	public static void main(String[] args) throws Exception {
		new Redeem().run();
	}
	
	
	Chain chain;
	void run() throws Exception {
		//config = MultiChainConfig.readFrom( "Prod-config");
		
		chain = new Chains().readOne( "Polygon", true);
		
		String wals = "0x1b4d5b59580844c9dc83c0f0c2d919c6348daad5 0x3e3cb35e4b86dafe644039428fe6e823b859b9f9 0x4cbc23a1b54e61a2709f44683f98b69e75bc17fd 0x6ab3231b899f0e4f292a271ad86f3b9d18a9e7ac 0x7707fa7b4f6a34b7f2b4fb3d439a71f8ff8b3760 0x78ecd4d8cb4b1390037f982998b81ac8e31e3878 0x7bd13a703a44d582080606204884b817335380f2 0x92199c0816090b579377207dc630ff71e0e2cb9f 0x9746c30412ac1714c46cd638d3645e915c8756bf 0x9e0fe0130892670edad78bc443bdb18088de116e 0x9e34eddef5f4012b342c9c7ad260241539efeb04 0xaa87366bdb347319cbeb89f3b2a1f91027fc0bc4 0xb61078fbe9bce27b41439e6647cf7bdd33d2d7e8 0xcb64aa2eb22489a60cb0c51b3f6b6d4656b602ee 0xf09430457f4dbd6593d73d02809bf3f0e32dd12a 0xf634b2b27ab469eae243d39e7a863953feea2a33";
		
		for (String wal : wals.split( " ")) {
			process( wal);
		}
	}

	private void process(String wal) throws Exception {
		double pos = chain.rusd().getPosition(wal);
		S.out( "%s %s", wal, "" + pos);
		
		if (pos < 100) {
			chain.rusd().sellRusd(wal, chain.busd(), pos).waitForReceipt();
		}

		// burn 500?
//		if (pos > 500) {
//			chain.rusd().burnRusd(wal, 500, chain.getAnyStockToken() ).waitForReceipt();
//		}
		
//		var json = MyClient.getJson( "https://reflection.trading/hook/polygon/get-wallet/" + wal);
//		
//		for (var posi : json.getArray( "positions") ) {
//			var tok = chain.getTokenByAddress( posi.getString( "address"));
//			double toks = posi.getDouble( "position");
//			if (tok != null && toks > 0) {
//				S.out( "POSITION %s\t%s\t%s", wal, tok.name(), posi.getDouble( "position") );
//				
//				var prices = MyClient.getJson( "https://reflection.trading/api/get-price/" + tok.conid() );
//				double bid = prices.getDouble( "bid");
//				if (bid > 0) {
//					chain.rusd().sellStockForRusd(wal, bid * toks, tok, toks).waitForReceipt();
//				}
//			}
//		}
		
	}
}
