package test;

import positions.MoralisServer;
import reflection.Config;
import testcase.Cookie;
import tw.util.S;

public class TestMoralis {
	
	
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		S.out( config.rusd().queryTotalSupply() );
		

	}
	
}
