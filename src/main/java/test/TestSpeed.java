package test;

import java.sql.ResultSet;

import http.MyClient;
import reflection.MySqlConnection;
import tw.util.S;

/** Compare db access to RefAPI access. */
public class TestSpeed {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MySqlConnection db = new MySqlConnection();
		db.connect(dbUrl, dbUser, dbPassword);
		
		for (int i = 0; i < 10; i++) {
			ResultSet ret = db.queryNext( "select * from events");
			S.out( " read " + ret.getString(3) );
		}
		S.out( "done");
		
	}
	
	
	public static void main2(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			S.out( "" + i);
			sendReq();
		}
		S.out( "done");  // 3 sec, 3+2  2+3
	}

	private static String sendReq() throws Exception {
		return MyClient.getString( "https://reflection.trading/api/?msg=getallstocks");
	}
}
