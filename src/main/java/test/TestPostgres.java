package test;

import org.json.simple.JSONObject;

import reflection.MySqlConnection;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			S.out( "usage: TestPostgres host port db_name user password");
			return;
		}
		
		String host = args[0];
		String port = args[1];
		String db = args[2];
		String user = args[3];
		String password = args[4];
		
		String url = String.format( "jdbc:postgresql://%s:%s/%s", host, port, db);
		S.out( "Connecting to: %s", url);
		
		MySqlConnection conn = new MySqlConnection();
		conn.connect( url, user, password);
		
//		JSONObject json = conn.queryToJson("select question, answer from frequently_asked_questions where is_active = true");
//		S.out( json);
	}
}
