package test;

import com.ib.client.OrderType;

import common.Util;
import tw.util.S;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		S.out( Util.getEnum( "LMT", OrderType.values() ) == OrderType.valueOf( "LMT") );

	}
}
