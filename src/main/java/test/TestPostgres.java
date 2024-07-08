package test;

import common.NiceTimer;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		NiceTimer t = new NiceTimer();

		for (int i = 0; i < 5000; i++) {
			t.schedule( 1000, () -> S.out( "exec") );
			S.sleep( 10);
		}
		
	}
}
