package test;

import common.Util;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	
	
	public static void main(String[] args) throws Exception {
		S.out( Util.toJson( 
				"a", 234,
				"b", "0xa"
				).getLong( "b") );
	}
}
