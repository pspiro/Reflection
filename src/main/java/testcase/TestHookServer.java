package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import positions.Streams;
import tw.util.S;

/** This test should be done in Dev or Prod only */
public class TestHookServer extends MyTestCase {
	String hook = "https://live.reflection.trading/hook";

	static {
		readStocks();
	}

	public void testTransfers() throws Exception {
		String wallet = Util.createFakeAddress();
		S.out( "testing with wallet %s", wallet);
		JsonObject json;
		
		json = MyClient.getJson( hook + "/get-wallet/" + wallet);
		
		m_config.rusd().mintRusd(wallet, 10, stocks.getAnyStockToken() ).waitForHash();
		
		for (int i = 0; i < 30; i++) {
			json = MyClient.getJson( hook + "/get-wallet/" + wallet);
			S.out( find( json.getArray("positions"), m_config.rusdAddr() ) );
			S.sleep(1000);
		}
		
//		m_config.rusd().burnRusd(wallet, 3, stocks.getAnyStockToken() ).waitForCompleted();
//		for (int i = 0; i < 10; i++) {
//			json = MyClient.getJson( hook + "/get-wallet/" + wallet);
//			S.out( find( json.getArray("positions"), m_config.rusdAddr() ) );
//			S.sleep(1000);
//		}
		
		
	}
	
	private double find(JsonArray array, String addr) {
		for (JsonObject item : array) {
			if (item.getString("address").equalsIgnoreCase(addr) ) {
				return item.getDouble("position");
			}
		}
		return 0;
	}

	public void testNative() {
		
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
