package test;

import positions.MoralisServer;
import reflection.Config;
import tw.util.S;

public class TestMoralis {
	static String chain = "0x5";
	//static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		S.out( MoralisServer.getWalletStats( Cookie.wallet) );
		
		S.out( MoralisServer.getErc20Stats( config.rusdAddr() ) );
		

	}
	
}
