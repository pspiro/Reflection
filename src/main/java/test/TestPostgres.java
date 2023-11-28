package test;

import http.MyClient;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		MyClient.getJson( "https://reflection.trading/refapi/ok").display();
	}
	
	
}
