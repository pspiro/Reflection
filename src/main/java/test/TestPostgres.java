package test;

import org.json.simple.JSONObject;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JSONObject obj = new JSONObject();
		obj.put("name", "peter");
		obj.put("active", true);
		
		config.sqlConnection( conn -> {
			conn.execute( "delete from users where name = 'peter'");
			conn.insertOrUpdate("users", obj, "name = '%s'", "peter");
			S.out(conn.queryToJson("select * from users where name = 'peter'"));

			obj.put("address", "smallville");
			conn.insertOrUpdate("users", obj, "name = '%s'", "peter");
			
			S.out(conn.queryToJson("select * from users where name = 'peter'"));
		});
		
		
		
	}
}
