package test;

import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		double ten18 = Math.pow(10, 18);
		double amt = Double.valueOf( "123400000000000000000") / ten18;
		S.out( amt);
		
	}
}
