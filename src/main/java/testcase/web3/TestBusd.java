package testcase.web3;

import common.Util;
import reflection.Config.Web3Type;
import testcase.MyTestCase;
import tw.util.S;

public class TestBusd extends MyTestCase {
	static String bobKey;
	static String bobAddr;
	
	static {
		try {
			bobKey = m_config.web3Type() == Web3Type.Fireblocks 
					? "bob" 
					: Util.createPrivateKey();
			bobAddr = m_config.matic().getAddress( bobKey);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testMint() throws Exception {
		S.out( "minting 200 BUSD into %s", bobAddr);
		m_config.busd().mint( bobAddr, 200).displayHash();

		S.out( m_config.busd().getPosition( bobAddr) );
		waitForBalance( bobAddr, m_config.busd().address(), 200, false);
		S.out( m_config.busd().getPosition( bobAddr) );
	}		

	/** this is failing due to insufficient gas but it shouldn't be more */
	public void testApprove() throws Exception {
		// transfer some gas to bob
		m_config.matic().transfer( m_config.ownerKey(), bobAddr, .005)
				.waitForHash();

		// let bob approve spending by spender
		String spender = Util.createFakeAddress();
		m_config.busd().approve( bobKey, spender, 50)
				.waitForHash();
		waitFor( 30, () -> m_config.busd().getAllowance(bobAddr, spender) > 49.99);
	}		
}
