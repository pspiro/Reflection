package test;

import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		JsonObject o = Util.toJson( "name", "zzz");
		Config config = Config.ask();
		config.sqlCommand( sql -> sql.insertJson("signup", o) );
		config.sqlQuery("select * from signup where name = 'zzz'").display();
	}
	
	
}
