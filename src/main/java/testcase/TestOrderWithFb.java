package testcase;

import java.sql.ResultSet;

import com.ib.client.Order;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.MyTransaction.Stablecoin;
import reflection.RefCode;
import tw.util.S;

public class TestOrderWithFb extends TestCase {
	static Config config;
	
	static {
		try {
			config = Config.readFrom("Dt-config");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// you must give some allowance to the wallet which means you must use Bob
//	public void testFillBuy() throws Exception {
//		MyJsonObject obj = TestOrder.orderData( 3, "BUY", 10);
//		obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
//		
//		MyJsonObject map = TestOrder.postDataToObj(obj);
//		String code = map.getString("code");
//		String text = map.getString("message");
//		S.out( "fill buy %s %s", code, text);
//		assertEquals( RefCode.OK.toString(), code);
//		double filled = map.getDouble( "filled");
//		assertEquals( 10.0, filled);
//
//		// this part won't work if Fireblocks is turned off
////		ResultSet res = TestOrder.config.sqlConnection().queryNext( "select * from crypto_transactions where id = (select max(id) from crypto_transactions)");
////		assertEquals( Cookie.wallet.toLowerCase(), res.getString("wallet_public_key").toLowerCase() ); 
////		long ts = res.getInt("timestamp");
////		long now = System.currentTimeMillis();
////		S.out( "  now=%s  timestamp=%s", new Date(now).toString(), new Date(ts * 1000).toString() );
////		assertTrue( now / 1000 - ts < 2000);
//		
//	}

	/** test inserting and reading back entry to crypto_transactions table */
	public void testCryptoTrans() throws Exception {
		config.sqlConnection( conn -> {
			conn.insertPairs("crypto_transactions",
				"crypto_transaction_id", "trans id",
				"timestamp", System.currentTimeMillis() / 1000, // why do we need this and also the other dates?
				"wallet_public_key", Cookie.wallet,
				"symbol", "SUCCESS",
				"conid", 8314,
				"action", "BUY",
				"quantity", 12.345,
				"price", 34.567,
				"commission", .1, // not so good, we should get it from the order. pas
				"spread", config.buySpread(),
				"currency", Stablecoin.BUSD.toString() );
		
			ResultSet ts = conn.queryNext( "select * from crypto_transactions where id = (select max(id) from crypto_transactions)");
			assertEquals("SUCCESS", ts.getString("symbol") );
		});
	}
	
}
