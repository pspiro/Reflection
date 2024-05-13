package testcase.web3;

import common.Util;
import reflection.Config.Web3Type;
import testcase.MyTestCase;
import tw.util.S;

public class TestBusd extends MyTestCase {
	public void testMint() throws Exception {
		String addr = Util.createFakeAddress();
		S.out( "minting 200 BUSD into %s", addr);
		m_config.busd().mint( addr, 200).displayHash();
		// position not showing up in hookserver
		S.out( m_config.busd().getPosition( "0x6f923e388ba9f37fdcd777ea93e586fa3ce841f2") );
		waitForBalance( addr, m_config.busd().address(), 200, false);
		S.out( m_config.busd().getPosition( "0x6f923e388ba9f37fdcd777ea93e586fa3ce841f2") );
	}		

	/** this is failing due to insufficient gas but it shouldn't be more */
	public void testApprove() throws Exception {
		String bobKey = m_config.web3Type() == Web3Type.Fireblocks 
				? "bob" 
				: Util.createPrivateKey();

		// transfer some gas to bob
		String caller = m_config.matic().getAddress( bobKey);
		m_config.matic().transfer( m_config.ownerKey(), caller, .005);

		// let bob approve spending by spender
		String spender = Util.createFakeAddress();
		m_config.busd().approve( bobKey, spender, 50)
				.waitForHash();
		waitFor( 30, () -> m_config.busd().getAllowance(caller, spender) > 49.99);
	}		
}
