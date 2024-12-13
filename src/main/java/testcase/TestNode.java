package testcase;

import java.util.Map;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import test.MyTimer;
import tw.util.S;
import web3.MoralisServer;
import web3.NodeInstance;

public class TestNode extends MyTestCase {
	public void testGetBlockNumber() throws Exception {
		assertTrue( node().getBlockNumber() > 0);
	}
	
	public void testGetFeeHistory() throws Exception {
		node().getFeeHistory( 5, 50).display();
	}

	/** This takes a long time and returns a lot of data */
//	public void testGetQueued() throws Exception {
//		node().getQueuedTrans().display();
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
		assertTrue( m_config.chain().busd().getAllowance( NodeInstance.prod, m_config.rusdAddr() ) >= 0); 
	}

	public void testGetDecimals() throws Exception {
		assertEquals( 6, node().getDecimals( m_config.rusdAddr() ) );
		assertEquals( 6, node().getDecimals( m_config.rusdAddr() ) );
		assertEquals( 18, node().getDecimals( m_config.chain().getAnyStockToken().address() ) );
	}
	
	public void testReqPosMap() throws Exception {
		S.out( "testing reqPosMap on " + chain().params().name() );
		
		MyTimer t = new MyTimer().next( "node");

		Map<String, Double> map1 = node().reqPositionsMap( 
				NodeInstance.prod,
				chain.getAllContractsAddresses(),
				18);

		t.next( "moralis");
		MoralisServer.setChain( chain().params().moralisPlatform() );
		Map<String, Double> map2 = MoralisServer.reqPositionsMap( 
				NodeInstance.prod,
				chain.getAllContractsAddresses() );
		
		t.done();
		
		assertTrue( Util.isEqual( map1, map2));

		new JsonObject( map1).display();
		assertTrue( map1.size() > 1);
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
			m_config.rusd().mintRusd( Cookie.wallet, 1, chain.getAnyStockToken() )
				.waitForReceipt();
		});
	}
}
