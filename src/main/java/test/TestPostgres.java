package test;

import java.util.Date;

import common.Util;
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
		
		String now = "2023-09-29 08:15:17.914349";//Util.yToS.format( new Date(System.currentTimeMillis() - 1000*60*60*24) );
		
		config.sqlQuery( conn -> conn.queryToJson("select * from transactions where created_at = '%s'", now) )
			.display();
		
	}
}
