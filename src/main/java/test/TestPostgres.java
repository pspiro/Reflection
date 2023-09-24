package test;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Object now = new Object() {
		@Override public String toString() {
			return "now()";
		}
	};

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JsonObject data0 = new JsonObject();
		data0.put("type", "jimm");
		
		JsonObject data1 = new JsonObject();
		data1.put("created_at", null);
		
		JsonObject data2 = new JsonObject();
		data2.put("created_at", now);
		
		config.sqlCommand( conn -> {
//			conn.insertJson("log", data0);
//			Util.pause();
//			conn.updateJson("log", data1, "type = '%s'", "jimm");
//			Util.pause();
			conn.updateJson("log", data2, "type = '%s'", "jimm");
		});
		
		JsonArray ar = config.sqlQuery( conn -> conn.queryToJson("select * from log where type = 'jimm'") );
		ar.display();
		
		
	}
}
// create a separate log table for exceptions? what is best way to insert call stack?