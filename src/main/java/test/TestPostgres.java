package test;

import org.json.simple.JsonObject;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Object now = new Object() {
		@Override public String toString() {
			return "now()";
		}
	};

	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		JsonObject obj = config.sqlQuery( conn -> conn.queryToJson("select * from crypto_transactions") ).get(0);
		S.out(obj);
		
		config.sqlCommand( sql -> sql.updateJson("crypto_transactions", obj, "uid=%s", obj.getString("uid")) );
		
	}
}
// create a separate log table for exceptions? what is best way to insert call stack?

