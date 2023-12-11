package test;

import positions.MoralisServer;
import reflection.Config;

public class TestMoralis {
	
	
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		MoralisServer.reqAllowance(
				config.busdAddr(), 
				"0x61a0b5510998f633063d4ad4e5e1d737a24dfb3c",
				"0x96531a61313fb1bef87833f38a9b2ebaa6ea57ce" 
				)
			.display();
		//S.out( config.rusd().queryTotalSupply() );
		

	}
	
}
