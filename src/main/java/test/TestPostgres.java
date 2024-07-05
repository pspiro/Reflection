package test;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config c;

	static {
		try {
			c = Config.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
//		c.matic().transfer(
//				c.ownerKey(), 
//				Util.createFakeAddress(),
//				.001).waitForHash();
//		c.busd().approve( c.ownerKey(),
//				"0x1cd8cd7607d1dd32915614bafc95834c5f2db3dc", c.rusdAddr() ) );
		S.out( c.busd().getAllowance( "0xda2c28af9cbfad9956333aba0fc3b482bc0aed13", c.rusdAddr() ) );
		S.out( c.busd().getAllowance( "0x7285420d377e98219ece3f004dd1d5fa33e9bbd9", c.rusdAddr() ) );
		S.out( c.busd().getAllowance( c.ownerAddr(), c.rusdAddr() ) );
	}
}
