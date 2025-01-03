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
		
		String wals = "0xd4983a7b8ee20247b27e7f1b39ba03846fe0336a";
		
		for (String wal : wals.split( " ")) {
			process( wal);
		}
	}

	private void process(String wal) throws Exception {
		double pos = chain.rusd().getPosition(wal);
		S.out( "%s %s", wal, "" + pos);
		
//		if (pos < 100) {
//			chain.rusd().sellRusd(wal, chain.busd(), pos).waitForReceipt();
//		}

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
