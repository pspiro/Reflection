package test;

import http.MyClient;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		S.out( MyClient.getString( "http://live.reflection.trading") );
	}
}
