package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExSupplier;
import fireblocks.Accounts;
import fireblocks.Fireblocks;
import http.MyClient;
import positions.Streams;
import tw.util.S;

/** This test should be done in Dev or Prod only */
public class TestHookServer extends MyTestCase {
	//String hook = "https://live.reflection.trading/hook";
	String hook = "http://localhost:8080/hook";

	static {
		readStocks();
	}

	public void testShow() throws Exception {
		String ownerWal = Accounts.instance.getAddress("Owner");
		S.out( ownerWal);
		MyClient.getJson( hook + "/get-wallet/0xdA2c28Af9CbfaD9956333Aba0Fc3B482bc0AeD13").display();
	}
		
	public void testTransfers() throws Exception {
		String wallet = Util.createFakeAddress();
		S.out( "testing with wallet %s", wallet);
		
		// let the HookServer start monitoring for this wallet
		MyClient.getJson( hook + "/get-wallet/" + wallet);
		
		// MINT
		m_config.rusd().mintRusd(wallet, 10, stocks.getAnyStockToken() ).waitForHash();

		tryFor( 60, () -> {
			JsonObject obj = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getArray("positions")
					.find( "address", m_config.rusdAddr() );
			return obj != null ? obj.getDouble("position") == 10 : false; 
		});

		// BURN
		m_config.rusd().burnRusd(wallet, 3, stocks.getAnyStockToken() ).waitForHash();
		
		tryFor( 60, () -> {
			JsonObject obj2 = MyClient.getJson( hook + "/get-wallet/" + wallet);
			JsonArray ar = obj2.getArray("positions");
			JsonObject obj = ar.find( "address", m_config.rusdAddr() );
			return obj != null ? obj.getDouble("position") == 7 : false;
		});

		// NATIVE
		Fireblocks.transfer(
				Accounts.instance.getId("Owner"), 
				wallet,
				Fireblocks.platformBase,
				.001, "test").waitForHash();
		
		tryFor( 60, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + wallet)
					.getDouble( "native");
			return Util.isEq( pos, .001, .00001);
		});			

		// APPROVE
		
		// start monitoring
		String ownerWal = Accounts.instance.getAddress("Owner");
		MyClient.getJson( hook + "/get-wallet/" + ownerWal);
		S.out( "approving for owner wallet " + ownerWal);
		
		int n = Util.rnd.nextInt( 1000) + 1;
		
		m_config.busd().approve(Accounts.instance.getId("Owner"), m_config.rusdAddr(), n)
				.waitForHash();

		tryFor( 120, () -> {
			double pos = MyClient.getJson( hook + "/get-wallet/" + ownerWal)
					.getDouble( "approved");
			return pos == n;
		});
	}

	/** wait n seconds for supplier to return true, then fail */
	void tryFor( int sec, ExSupplier<Boolean> sup) throws Exception {
		for (int i = 0; i < sec; i++) {
			S.out( i);
			if (sup.get() ) {
				return;
			}
			S.sleep(1000);
		}
		assertTrue( false);
	}
		
	public void testApproval() throws Exception {
		String id = Streams.createStreamWithAddresses(testStream1);
		S.out( "created " + id);
		
		String id2 = Streams.createStreamWithAddresses(testStream1);
		S.out( "created " + id2);
		
		assertEquals( id, id2);
		
		Streams.deleteStream(id);		
	}
	
	static String testStream1 = """
	{
		"description": "TestStream1",
		"webhookUrl" : "http://108.6.23.121/hook/webhook",
		"chainIds": [ "0x5" ],
		"tag": "teststream1",
		"demo": true,
		"includeNativeTxs": true,
		"allAddresses": false,
		"includeContractLogs": false,
		"includeInternalTxs": false,
		"includeAllTxLogs": false
	}
	""";
	
}
