package test;

import positions.MoralisServer;
import tw.util.S;

public class TestMoralis {
	static String chain = "0x5";
	static String apple = "0x29c6f774536dFc3343e2e8D804Ed233690083299";
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			S.out( "query");
			MoralisServer.reqPositions(myWallet);
			S.out( "  done");
		}
	}
	
}
