package testcase.web3;

import common.Util;
import positions.Wallet;
import testcase.MyTestCase;
import tw.util.S;
import web3.Matic;

public class TestBusd extends MyTestCase {
	public void testMint() throws Exception {
		String addr = Util.createFakeAddress();
		S.out( "minting 200 BUSD into %s", addr);
		m_config.busd().mint( addr, 200).displayHash();
		// position not showing up in hookserver
		S.out( new Wallet("0x6f923e388ba9f37fdcd777ea93e586fa3ce841f2").getBalance(
				m_config.busd().address() ) );
		
		waitForBalance( addr, m_config.busd().address(), 200, false);

		S.out( new Wallet("0x6f923e388ba9f37fdcd777ea93e586fa3ce841f2").getBalance(
				m_config.busd().address() ) );
	}		

	/** this is failing due to insufficient gas but it shouldn't be more */
	public void testApprove() throws Exception {
		String callerKey = Util.createPrivateKey();
		String caller = Matic.getAddress( callerKey);
		m_config.matic().send( m_config.ownerKey(), caller, .005);  // why so high!!!???

		String spender = Util.createFakeAddress();
		m_config.busd().approve( callerKey, spender, 50)
				.waitForHash();
		waitFor( 30, () -> m_config.busd().getAllowance(caller, spender) > 49.99);
	}		
}
