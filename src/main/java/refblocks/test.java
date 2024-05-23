package refblocks;

import common.Util;
import reflection.Config;

public class test {
	static String someKey = "bdc76b290316a836a01af129884bdf9a1977b81ae5a7a0f1d8b5ded4d9dcee4d";
	
	public static void main(String[] args) throws Exception {
		Config c = Config.ask( "Dt");
		RbRusd rusd = new RbRusd( c.rusdAddr(), 6);
		rusd.addOrRemoveAdmin(
				someKey,
				Util.createFakeAddress(),
				true);
	}
}
