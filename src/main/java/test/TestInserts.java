package test;

import reflection.MySqlConnection;
import reflection.Util;

public class TestInserts { //extends TestCase {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";

//	String sql = "create table commissions ("   // in Java 13 you have text blocks, you wouldn't need all the + "
//			+ "tradekey varchar(42),"
//			+ "commission double precision,"
//			+ "currency varchar(3)"
//			+ ")";
	
	static MySqlConnection c = new MySqlConnection();

	public static void main(String[] args) throws Exception {
		testInsert1();
		testUpdate1();
	}
	
	/** The insert() function takes care of the single-quotes for us. */
	static void testInsert1() throws Exception {
		c.connect(dbUrl, dbUser, dbPassword);
		
		c.insert( "commissions", "trade'key1", 3, "USD");
		
		String[] cols = { "commission", "tradekey", "currency" };
		c.insert( "commissions", cols, 4, "trade'key2", "XYZ");
		c.insert( "commissions", cols, 4, "trade'key3", "DEF");
	}

	/** For updates, we must double-up the single-quotes. */
	static void testUpdate1() throws Exception {
		c.connect(dbUrl, dbUser, dbPassword);
		
		c.execute( String.format( "update commissions set tradekey = '%s' where tradekey = '%s'",
				Util.dblQ("trade'key4"), Util.dblQ("trade'key3") ) );
	}
}
