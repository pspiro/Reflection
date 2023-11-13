package test;

import reflection.Config;
import tw.util.S;

public class TestMoralis {
	
	
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		S.out( config.rusd().queryTotalSupply() );
		

	}
	
}
