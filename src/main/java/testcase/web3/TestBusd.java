package testcase.web3;

import common.Util;
import testcase.MyTestCase;
import tw.util.S;

public class TestBusd extends MyTestCase {
	public void testTransferFrom() throws Exception {
		String user = chain().params().refWalletAddr();
		String key = chain().params().refWalletKey();
		
		// approver: user
		// spender: owner
		
		// approve
		chain().busd().approve( key, chain().params().ownerAddr(), 100)
			.waitForReceipt();
		
		// mint
		chain().busd().mint( key, user, 100).waitForReceipt();
		waitForBalance( user, m_config.busd().address(), 100, false);
		
		// check balance and approval
		double pos = chain().busd().getPosition( user);
		assertTrue( pos >= 100);

		double appr = chain().busd().getAllowance( user, chain().params().ownerAddr() );
		assertTrue( appr == 100);
		
		S.out( "busd bal=%s  approved=%s", pos, appr);
		
		chain().busd().transferFrom( chain().params().ownerKey(), user, chain().params().refWalletAddr(), 1)
			.waitForReceipt();
	}
}
