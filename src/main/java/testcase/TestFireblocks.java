package testcase;

import java.math.BigInteger;

import fireblocks.Fireblocks;
import junit.framework.TestCase;

public class TestFireblocks extends TestCase {
	
	String ge =       "0x7abc82771a6afa4d0d56045cf09cb1deaedb3cc2";
	String userAddr = "0xAb52e8f017fBD6C7708c7C90C0204966690e7Fc8"; // Testnet Test1 account (id=1)	
	String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	String myStock =  "0x0b55eeb4a4d9a709b1144b6991c463e9ff10648d"; // deployed with RUSD w/ two RefWallets

	public void testEncode() throws Exception {
		String[] types = { "string", "address", "uint256", "uint256" };
		Object[] vals = {
				"hello",
				myWallet,
				3,
				new BigInteger("4")
		};
		
		assertEquals( 
				"0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000b016711702D3302ceF6cEb62419abBeF5c44450e00000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000568656c6c6f000000000000000000000000000000000000000000000000000000",
				Fireblocks.encodeParameters( types, vals) );
	}
	
	public void testStringToBytes() {
		assertEquals( "414243", Fireblocks.stringToBytes("ABC") );		
	}
}
