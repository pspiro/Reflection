package testcase.web3;

import common.Util;
import testcase.MyTestCase;

public class TestSendEth extends MyTestCase {
	public void test() throws Exception {
		String key = Util.createPrivateKey();
		String wallet = m_config.matic().getAddress( key);
		
		// send from owner to wallet
		m_config.matic().transfer( m_config.ownerKey(), wallet, .005);   
		m_config.matic().transfer( m_config.ownerKey(), wallet, .005);   
		m_config.matic().transfer( m_config.ownerKey(), wallet, .005).displayHash();  // it fails if you don't wait, as it should   
		
		// then send it back; allow some extra for gas
		m_config.matic().transfer( key, m_config.ownerAddr(), .01).displayHash();
	}
}
