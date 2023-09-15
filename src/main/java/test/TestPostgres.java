package test;

import org.json.simple.JsonObject;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JsonObject obj = new JsonObject();
		obj.put("name", "peter");
		
		config.sqlCommand( conn -> {
			conn.execute( "delete from users where name = 'peter'");
			conn.insertOrUpdate("users", obj, "name = '%s'", "peter");
			S.out(conn.queryToJson("select * from users where name = 'peter'"));

			obj.put("address", "smallville");
			conn.insertOrUpdate("users", obj, "name = '%s'", "peter");
			
			S.out(conn.queryToJson("select * from users where name = 'peter'"));
		});
		
		
		
	}
}
