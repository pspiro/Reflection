package testcase;

import java.util.Map;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.NodeServer;

public class TestNodeServer extends MyTestCase {
	public void testGetBlockNumber() throws Exception {
		assertTrue( NodeServer.getBlockNumber() > 0);
	}
	
	public void testGetFeeHistory() throws Exception {
		NodeServer.getFeeHistory( 5, 50).display();
	}

	/** This takes a long time and returns a lot of data */
//	public void testGetQueued() throws Exception {
//		NodeServer.getQueuedTrans().display();
//	}
	
	public void testGetNativeBal() throws Exception {
		assertTrue( NodeServer.getNativeBalance( NodeServer.prod) > 0);
	}
	
	public void testQueryFees() throws Exception {
		S.out( NodeServer.queryFees() );
	}
	
	public void testGetLatestBlock() throws Exception {
		NodeServer.getLatestBlock().display();
	}
	
	public void testGetBalance() throws Exception {
		assertTrue( NodeServer.getBalance( m_config.rusdAddr(), NodeServer.prod, 6) > 0);
	}
	
	public void testGetTotalSupply() throws Exception {
		assertTrue( NodeServer.getTotalSupply( m_config.rusdAddr(), 6) > 0);
	}
	
	public void testGetAllowance() throws Exception {
		assertTrue( NodeServer.getAllowance( m_config.rusdAddr(), NodeServer.prod, NodeServer.prod, 6) >= 0); 
	}

	public void testGetDecimals() throws Exception {
		assertEquals( 6, NodeServer.getDecimals( m_config.rusdAddr() ) );
		assertEquals( 6, NodeServer.getDecimals( m_config.rusdAddr() ) );
	}
	
	public void testReqPosMap() throws Exception {
		Map<String, Double> map = NodeServer.reqPositionsMap( 
				NodeServer.prod,
				stocks.getAllContractsAddresses(),
				18);
		new JsonObject( map).display();
		assertTrue( map.size() > 1);
	}
	
	/** fail w/ batch too large 
	 * @throws Exception */
	public void testBatchFail() throws Exception {
		String body = """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "eth_getBlockByNumber",
				"params": [	"latest", false ]
				}""";
		var json = JsonObject.parse( body);
		var ar = new JsonArray();
		for (int i = 0; i < 3; i++) {
			json.put( "id", i);
			ar.add( json);
		}
		NodeServer.batchQuery( ar).display();
	}

	public static void testKnownTrans() throws Exception {
		assertTrue( NodeServer.isKnownTransaction( 
				"0x24f1aab3b5bca3cd526c3c65b28267133f2f0b0540501271164a42d6d5661915") ); // passes on Sepolia only
		assertFalse( NodeServer.isKnownTransaction( 
				"0x24f1aab3b5bca3cd526c3c65b28267133f2f0b0540501271164a42d6d5661916") );
	}

	public void test() throws Exception {
		Util.wrap( () -> {
			m_config.rusd().mintRusd( Cookie.wallet, 1, stocks.getAnyStockToken() )
				.waitForHash();
		});
	}
}
