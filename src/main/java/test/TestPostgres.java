package test;

import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;


/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		
		JsonObject obj = Util.toJson( 
				"tradekey", "mytradekey",
				"comm_paid", 1.2);

		config.sqlCommand( conn -> conn.insertJson( "commissions", obj) );
	}
}
