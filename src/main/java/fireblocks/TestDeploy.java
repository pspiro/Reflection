package fireblocks;

import reflection.Config;
import testcase.MyTestCase;

public class TestDeploy {
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dev-config");
		
		config.rusd().deploy("c:/work/smart-contracts/build/contracts/rusd.json", 
				MyTestCase.dead, MyTestCase.dead);
	}
	
	
}

