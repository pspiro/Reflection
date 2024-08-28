package testcase;

import java.util.ArrayList;
import java.util.Arrays;

import tw.util.S;
import web3.NodeServer;

public class TestReqPositionsMap extends MyTestCase {

	/** fails in production, batch size too large */
	public void testBatch() throws Exception {
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		var allContracts = list.toArray( new String[list.size()]);
		
		//allContracts = trim( allContracts, 11);
	
		String wallet = "0x4c5f126Bc37d449944eDC343383be665F315d54A";
		var map = NodeServer.reqPositionsMap( wallet, allContracts, 0);
		S.out( map);
		
	}

	private String[] trim(String[] all, int len) {
		String[] ar = new String[len];
		for (int i = 0; i < len; i++) {
			ar[i] = all[i];
		}
		return ar;
	}
}
