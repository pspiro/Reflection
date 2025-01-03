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
		
		String wals = "0xa14749d89e1ad2a4de15ca4463cd903842ffc15d,0x288798d619d24ee937d0a88e0c6cd14f67a3eb0c,0x71d85c2fcab3c1e581843eac204aef22eb9e93d7,0xa7e7ebb00ceca94e21413121c09cbfeab74d388b,0x22b61d3035af7e46bb5a6082741dcbf6ff037d09,0x48a8ed47f44d067ccf28be12ba648ea1c2810ce3,0xd1346e10832af0c9da0f0d2adf44c348f225aa6d,0xab24a23206c15b10fe6d908d7ae22593ecfd7b68,0x8cb789689d1f740429875db6393a1ac59553bfed,0xf2264537e817b0fc6f62c22d3cdfdc0ab3d3b2e2,0x96b7e76d2dbc4c211e2c2b65369ab18b46e76628,0x300ad421ced88d841d601c2dcca2f13873666468,0x6e08ac33861020f3c6f665301daff84ca5cc45bf,0x2f5204608b4f11495056a4a99821d7945a118c88,0xc1f8b839c908378a81ef6dfb9ed29d9b4ab54862,0xd540d67fb1de697ca663ea8676c8527ee0963a5e,0xa40641f764540aa1bb9217a4940d9c12547f41e3,0xd1fd45419bcd2f02c2db85fd6dc34357f401ba8c,0x683037c53cefb12215b88e42022f37d7606dce92,0x706e924389e91c8fa37398754c5f76305c652315,0x55e2a56f25e79426e03037145870ba49211c9302,0x8a157b7e93e1ba6321c35b030060b5a9223ff8e3,0xde1d7ed9448ef150deb3e6dd7bc19fdc01b66001,0x504eda4ca097d33981902cdede7f0e55e9092451,0xfa71d8ef4ed81a7f96736e78bfa3dba06fdf6402,0x180aafb2ebbe4bcfd2671a6caa4b47dead77a25e,0xd4c60c3f364e5b3d6ec0f81d76c782905e413f5a,0x25b76dc75f4e4357365c4cb350fefb568f7f42a0";
		
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
