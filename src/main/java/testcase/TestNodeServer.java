package testcase;

import java.util.Map;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.NodeInstance;

public class TestNodeServer extends MyTestCase {
	static NodeInstance node() {
		return m_config.node();
	}

	public void testGetBlockNumber() throws Exception {
		assertTrue( node().getBlockNumber() > 0);
	}
	
	public void testGetFeeHistory() throws Exception {
		node().getFeeHistory( 5, 50).display();
	}

	/** This takes a long time and returns a lot of data */
//	public void testGetQueued() throws Exception {
//		NodeServer.getQueuedTrans().display();
//	}
	
	public void testGetNativeBal() throws Exception {
		assertTrue( node().getNativeBalance( NodeInstance.prod) > 0);
	}
	
	public void testQueryFees() throws Exception {
		S.out( node().queryFees() );
	}
	
	public void testGetLatestBlock() throws Exception {
		node().getLatestBlock().display();
	}
	
	public void testGetBalance() throws Exception {
		assertTrue( node().getBalance( m_config.rusdAddr(), NodeInstance.prod, 6) > 0);
	}
	
	public void testGetTotalSupply() throws Exception {
		assertTrue( node().getTotalSupply( m_config.rusdAddr(), 6) > 0);
	}
	
	public void testGetAllowance() throws Exception {
		assertTrue( node().getAllowance( m_config.rusdAddr(), NodeInstance.prod, NodeInstance.prod, 6) >= 0); 
	}

	public void testGetDecimals() throws Exception {
		assertEquals( 6, node().getDecimals( m_config.rusdAddr() ) );
		assertEquals( 6, node().getDecimals( m_config.rusdAddr() ) );
	}
	
	public void testReqPosMap() throws Exception {
		Map<String, Double> map = node().reqPositionsMap( 
				NodeInstance.prod,
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
		node().batchQuery( ar).display();
	}

	public static void testKnownTrans() throws Exception {
		assertTrue( node().isKnownTransaction( 
				"0x24f1aab3b5bca3cd526c3c65b28267133f2f0b0540501271164a42d6d5661915") ); // passes on Sepolia only
		assertFalse( node().isKnownTransaction( 
				"0x24f1aab3b5bca3cd526c3c65b28267133f2f0b0540501271164a42d6d5661916") );
	}

	public void test() throws Exception {
		Util.wrap( () -> {
			m_config.rusd().mintRusd( Cookie.wallet, 1, stocks.getAnyStockToken() )
				.waitForHash();
		});
	}
}
