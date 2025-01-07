package test;

import org.json.simple.JsonObject;

import chain.Chain;
import chain.Chains;
import common.Util;
import reflection.Config.MultiChainConfig;
import reflection.MySqlConnection.MySqlDate;
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
		
		//String wals = "0x01b70f48090b2d44ad541afdcacd95fcd5656e2c,0x0241fd8a4ff428c0926999e7c8cbda65f19766b0,0x2991c02ca5260600701cd61a8a2a9b5cbc194efc,0x2b7012a93c867d2504e8781c2e259ba100bdb0d0,0x7bab95194d3bab87d39e9f4d2870c560f38dc6b2,0x81d4aec94ca6ed6fd0c3b8b2decf63275c1edcc0,0xa7b8ec6c2bcd1f5e665ee984747dd4230eae027d,0xb701a14c58ae69ddc82d0051cd81b4e20a5d0ed7,0xbc770adfb405c86526b8560ab1902007f1532dad,0xbe05d0e485cc2f15bb5d5ecbf78e445cfbe1ff81,0xc8847ddf23e2075b0a32701fe971c747128f445c,0xd1346e10832af0c9da0f0d2adf44c348f225aa6d,0xd4983a7b8ee20247b27e7f1b39ba03846fe0336a,0xd4c60c3f364e5b3d6ec0f81d76c782905e413f5a,0xde1d7ed9448ef150deb3e6dd7bc19fdc01b66001,0xdfbb1f87a1ccd1e0831f975642e189a529274a54,0xe5a746d40998277dc1abe21d90128deb56f87f58,0xecdf5ba9aa8f1d98e2af9c5a094d9571ef7f5155,0xed29529cab8542ae405b4b47588dbd212bbf7ea8,0xef932bbd8e4e44358eb5546110d046c1c9fd38ac,0xf1661d2a9a48b57ba548830ad21d85d77a534b79,0xf595ecdf1b3be287b05b3e20f2e95a471bfb6be4";
		String wals = "0x01b70f48090b2d44ad541afdcacd95fcd5656e2c,0x0241fd8a4ff428c0926999e7c8cbda65f19766b0,0x2991c02ca5260600701cd61a8a2a9b5cbc194efc,0x81d4aec94ca6ed6fd0c3b8b2decf63275c1edcc0,0xb701a14c58ae69ddc82d0051cd81b4e20a5d0ed7,0xf1661d2a9a48b57ba548830ad21d85d77a534b79";
		
		for (String wal : wals.split( ",")) {
			process( wal);
		}
	}

	private void process(String wal) throws Exception {
		S.out();
		var user = config.sqlQueryOne( "select * from users where wallet_public_key = '%s'", wal);
		
		if (user == null) {
			S.out( "error: wallet %s not found", wal);
			return;
		}
		
		double rusd = chain.rusd().getPosition(wal);
		S.out( "wallet=%s  bal=%s  first=%s  country=%s  email=%s", wal, rusd, user.getString( "first_name"), user.getString( "country"), user.getString( "email") );
		
		// burn 500?
		if (rusd > 500) {
			chain.rusd().burnRusd(wal, 500, chain.getAnyStockToken() ).waitForReceipt();
			rusd -= 500;
		}
		
		if (rusd < 40) {
			S.out( "redeem " + rusd + " for " + wal);
			var hash = chain.rusd().sellRusd(wal, chain.busd(), rusd).waitForReceipt();

			JsonObject obj = new JsonObject();
			obj.put( "created_at", new MySqlDate() );  // we want created_at to be updated on updates
			obj.put( "uid", Util.uid( 8) );
			obj.put( "wallet_public_key", wal.toLowerCase() );
			obj.put( "chainId", chain.chainId() );
			obj.put( "stablecoin", chain.busd().name() );
			obj.put( "amount", rusd);
			obj.put( "status", "Completed");
			obj.put( "blockchain_hash", hash);

			// only allow one "working" redemption at a time
			config.sqlCommand( conn -> conn.insertOrUpdate(
					"redemptions", 
					obj,
					"wallet_public_key = '%s'",
					wal.toLowerCase() ) );
		}
	}
	
}
