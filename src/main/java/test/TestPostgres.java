package test;

import org.json.simple.JSONObject;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JSONObject obj = new JSONObject();
		obj.put("name", "peter");
		obj.put("active", true);
		
		config.sqlConnection( conn -> {
			conn.insertJson("users", obj);
			
			obj.put("address", "smallville");
			conn.updateJson("users", obj, "name = 'peter'");
		});
		
		
		
	}
}
