package test;

import positions.MoralisServer;
import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config c;

	static {
		try {
			c = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		long pos = MoralisServer.getBalance( "0x2703161D6DD37301CEd98ff717795E14427a462B", "0x455759A3F9124Bf2576dA81fb9ae8e76B27fF2D6");
		S.out( pos);
	}
}
