package test;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import reflection.Config;
import tw.util.S;
import util.LogType;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JsonObject data = new JsonObject();
		data.put("name", "peter");
		//data.put("type", LogType.REC_ORDER);
		
//		config.sqlCommand( conn -> {
//			String[] cols = { "wallet_public_key", "uid", "type", "data" };
//			Object[] vals = { "my wallet", "my uid", "my type", data };
//			conn.insert( "log", cols, vals);
//		});
		
		JsonObject json = new JsonObject();
		json.put("type", LogType.REC_ORDER);
		json.put("type", LogType.REC_ORDER);
		S.out( json);
		
		config.sqlCommand( conn -> {
			conn.insertJson("log", json);
		});
		
		JsonArray ar = config.sqlQuery( conn -> conn.queryToJson("select * from log") );
		ar.display();
		
		
	}
}
// create a separate log table for exceptions? what is best way to insert call stack?