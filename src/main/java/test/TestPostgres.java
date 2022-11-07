package test;

import java.sql.SQLException;

import reflection.MySqlConnection;
import tw.util.S;

public class TestPostgres {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";

	public static void main(String[] args) throws SQLException {
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
		S.out( "Completed");
	}
}
