package testcase;

import common.Util;
import tw.util.S;

public class TestTransferFrom extends MyTestCase {
	public void test(String[] args) throws Exception {
		String dest = Util.createFakeAddress();
		
		// let admin send money from owner to dest
		
// if needed:
//		m_config.busd().mint( m_config.ownerKey(), m_config.ownerAddr(), 100000)
//			.waitForReceipt();
				
		// let owner approve admin to send money
		m_config.busd().approve( m_config.ownerKey(), chain().params().sysAdminAddr(), 200)
			.waitForReceipt();
		
		double al = m_config.busd().getAllowance( m_config.ownerAddr(), chain().params().sysAdminAddr() );

		S.out( "old balance: " + m_config.busd().getPosition(m_config.ownerAddr()));
		S.out( "old allowance: " + al);

		// let admin send money from owner to dest
		m_config.busd().transferFrom( m_config.admin1Key(), m_config.ownerAddr(), dest, 1)
			.waitForReceipt();
		
		S.out( "transfer completed");
		
		al = m_config.busd().getAllowance( m_config.ownerAddr(), chain().params().sysAdminAddr() );
		S.out( "new balance: " + m_config.busd().getPosition(m_config.ownerAddr() ) );
		S.out( "new allowance: " + al);
		
	}
}
