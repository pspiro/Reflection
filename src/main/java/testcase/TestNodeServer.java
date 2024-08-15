package testcase;

import java.util.Map;

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
		assertTrue( map.size() == stocks.stocks().size() );
	}
}
