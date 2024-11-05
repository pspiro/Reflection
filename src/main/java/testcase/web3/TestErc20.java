package testcase.web3;

import common.Util;
import testcase.MyTestCase;
import tw.util.S;

public class TestErc20 extends MyTestCase {
	
	public void test_getAllBalances() throws Exception {
		var bal = chain().rusd().getAllBalances();
		S.out( "all balances:");
		S.out( bal);
		assertTrue( bal.size() > 0);
	}
	
	public void test_queryTotalSupply() throws Exception {
		double v = chain().busd().queryTotalSupply();
		S.out( "total supply: " + v);
		assertTrue( v > 0);
	}
	
	public void test_getAllowance() throws Exception {
		double v = chain().getApprovedAmt();
		S.out( "allowance: " + v);
		assertTrue( v > 0);
	}

	/** you could use Moralis owners endpoint to check, ours doesn't work */
	// only this one is failing. bc
	public void test_getsetOwner() throws Exception {
		String owner = chain().busd().getOwner();
		assertEquals( m_config.ownerAddr(), owner); 
	}
	
	/** you could use Moralis owners endpoint to check, ours doesn't work */
	public void test_setOwner() throws Exception {
		// don't set new owner?
//		String key = Util.createPrivateKey();
//		String addr = Util.getAddress( key);
//		chain().busd().setOwner( chain().params().ownerKey(), addr);
//		assertEquals( addr, node().getOwner( chain().busd().address() ) );
	}
	
	public void test_approve() throws Exception {
		m_config.busd().approve( m_config.refWalletKey(), m_config.rusdAddr(), 1234).waitForReceipt();
		double v = chain().getApprovedAmt();
		assertEquals( 1234., v);
	}
	
	public void test_mint_and_position() throws Exception {
		String wal = Util.createFakeAddress();
		m_config.mintBusd( wal, 55).waitForReceipt();
		double v = chain().busd().getPosition(wal);
		assertEquals( 55., v);
	}
	
	public void test_transfer() {
		assertTrue( false); // write this
	}
	
	public void test_transferFrom() {
		assertTrue( false); // write this
	}
}
