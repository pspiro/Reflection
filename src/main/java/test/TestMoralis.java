package test;

import reflection.Config;
import testcase.Cookie;
import tw.util.S;

public class TestMoralis {
	static String chain = "0x5";
	//static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String wallet = "0xb95bf9c71e030fa3d8c0940456972885db60843f";
	
	
	public static void main(String[] args) throws Exception {
		//pos = MoralisServer.reqPositions(wallet);
		//S.out( StockToken.fromBlockchainHex("145660ddc59b3fc", 6) );
		Config config = Config.readFrom("Desktop-config");
		S.out( config.newBusd().getAllowance(wallet, config.rusdAddr() ) );

	}
	
}
