package test;

import common.Util;
import tw.util.S;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		String a = "0.9999959732572044";
		double v = Double.valueOf( a);
		
		double t = Util.truncate( v, 4);
		S.out( "" + v + " " + t);
		
		S.out( "%s %s",
				S.fmt4( v), S.fmt4( t) );
		
	}
}
