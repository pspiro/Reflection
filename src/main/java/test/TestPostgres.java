package test;

import java.sql.ResultSet;

import reflection.Config;
import reflection.MySqlConnection;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		MySqlConnection sql = config.sqlConnection();
		
		sql.insertPairs( "events", 
				"block", 32,
				"token", "my token"
				);

		ResultSet res = sql.queryNext( "select * from events where block = 32");
		S.out( "%s %s", res.getInt(1), res.getString(2) );
	}
}
