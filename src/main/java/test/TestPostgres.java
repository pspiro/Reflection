package test;

import fireblocks.Accounts;
import http.MyClient;
import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		Config.ask().busd().approve(
				Accounts.instance.getId("Admin1"), 
				"0x2703161D6DD37301CEd98ff717795E14427a462B",
				1);
	}
}
