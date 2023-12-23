package test;

import common.Util;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		double v = .10000000001;
		float f = 1.123456789F;
		S.out(v);
		S.out( Util.toJson( "val", v, "float", f) );
		Util.toJson( "val", v, "float", f).display();
	}
}
