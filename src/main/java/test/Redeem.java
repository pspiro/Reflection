package test;

import chain.Chain;
import chain.Chains;
import http.MyClient;
import reflection.Config.MultiChainConfig;
import reflection.SingleChainConfig;
import tw.util.S;

/** figure out who should be redeemed and how much */
public class Redeem {

	public static void main(String[] args) throws Exception {
		new Redeem().run();
	}
	
	Chain chain;
	private SingleChainConfig config;
	
	void run() throws Exception {
		config = MultiChainConfig.readFrom( "Prod-config");
		
		chain = new Chains().readOne( "Polygon", true);
		
		String wals = "0xd4983a7b8ee20247b27e7f1b39ba03846fe0336a";
		
		for (String wal : wals.split( ",")) {
			process( wal);
		}
	}

	private void process(String wal) throws Exception {
		S.out();
		var obj = config.sqlQueryOne( "select * from users where wallet_public_key = '%s'", wal);
		
		if (obj == null) {
			S.out( "error: wallet %s not found", wal);
			return;
		}
		
		S.out( "wallet=%s  first=%s  country=%s  email=%s", wal, obj.getString( "first_name"), obj.getString( "country"), obj.getString( "email") );
		
		var resp = MyClient.getJson( "https://reflection.trading/hook/polygon/get-wallet/" + wal);
		var ar = resp.getArray( "positions");
		ar.forEach( posPair -> {
			String addr = posPair.getString( "address");
			double pos = posPair.getDouble( "position");
			S.out( "%s %s", chain.getName( addr), pos);
		});
		
		double rusd = chain.rusd().getPosition(wal);
		
		// burn 500?
		if (rusd > 500) {
			chain.rusd().burnRusd(wal, 500, chain.getAnyStockToken() ).waitForReceipt();
			rusd -= 500;
		}
		// burn 100?
		else if (rusd > 100) {
			chain.rusd().burnRusd(wal, 100, chain.getAnyStockToken() ).waitForReceipt();
			rusd -= 100;
		}
		
		// redeem
		if (rusd < 200) {
			chain.rusd().sellRusd(wal, chain.busd(), rusd).waitForReceipt();
			config.sqlCommand( sql -> sql.execWithParams(
					"update redemptions set status = 'Completed' where wallet_public_key = '%s'", wal) );
		}

//		if (pos < 100) {
//			chain.rusd().sellRusd(wal, chain.busd(), pos).waitForReceipt();
//		}



		// close out positions
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
