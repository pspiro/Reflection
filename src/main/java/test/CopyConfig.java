package test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import reflection.MySqlConnection;
import tw.util.S;

/** Copy any table from one postgres to another */
public class CopyConfig {
	static String dbUrl1 = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUrl2 = "jdbc:postgresql://34.86.193.58:5432/reflection_prod";
	static String dbUser = "postgres";
	static String dbPassword = "1359";
	//static String tableName = "system_configurations";
	static String tableName = "frequently_asked_questions";

	MySqlConnection c1 = new MySqlConnection();
	MySqlConnection c2 = new MySqlConnection();
	
	public static void main(String[] args) throws Exception {
		new CopyConfig().run();
		S.out( "done");
	}

	private void run() throws Exception {
		S.out( "Copying data");
		S.out( "From: %s %s %s", dbUrl1, dbUser, tableName);
		S.out( "To:   %s %s %s", dbUrl2, dbUser, tableName);
		c1.connect( dbUrl1, dbUser, dbPassword);

		ResultSet res = c1.query( "select * from %s", tableName);
		while (res.next() ) {
			
			ResultSetMetaData meta = res.getMetaData();
			meta.getColumnCount();
			
			Object[] vals = new String[meta.getColumnCount()];
			for (int i = 0; i < meta.getColumnCount(); i++) {
				vals[i] = res.getString(i+1);
				S.out( "%s  %s", meta.getColumnName(i+1), vals[i]);
			}
			 
			c2.connect( dbUrl2, dbUser, dbPassword);
			//c2.insert(tableName, vals);
		}
	}
}
