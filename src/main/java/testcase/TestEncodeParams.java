package testcase;

import common.Util;
import tw.util.S;
import web3.Param;
import web3.Param.Address;
import web3.Param.BigInt;

public class TestEncodeParams extends MyTestCase {
	public void test() throws Exception {
		String addr1 = Util.createFakeAddress();
		String addr2 = Util.createFakeAddress();
		int amount = 255;
		
		Param[] params = {
				new Address( addr1),
				new Address( addr2),
				new BigInt( amount)
		};
		
		String p1 = Param.encodeData( "keccak", params);
		S.out( p1);
		
		
		String p2 = Param.encodeParameters(
				Util.toArray( "address", "address", "uint256"),
				Util.toArray( addr1, addr2, amount) );
		S.out( p2);
		
		assertEquals( p2, p1);
	}
}
