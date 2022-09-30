package test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.Config;
import reflection.MySqlConnection;
import tw.util.S;

public class TestPostgres extends TestCase {
	static String dbUrl = "jdbc:postgresql://localhost:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";
	static MySqlConnection con = new MySqlConnection();

	public static void main(String[] args) {
		try {
			con.connect(dbUrl, dbUser, dbPassword);
			
			new TestPostgres().createTrades();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		S.out( "done");
	}
	
	void createProfit() throws SQLException {
		String sql = "create table profit ("   // in Java 13 you have text blocks, you wouldn't need all the + "
				+ "crypto_id varchar(32)"
				+ "wallet_id varchar(32)"
				+ "region varchar(32)"
				+ "side varchar(4)"
				+ "symbol varchar(10)"
				+ "conid integer"
				+ "commission double precision,"
				+ "crypto_qty double precision"
				+ "crypto_price double precision,"
				+ "exchange_qty"
				+ "exchange_price double precision,"
				+ "profit double precision"
				+ ")";
		
		con.execute( sql);
	}
	
	void createConfig() throws SQLException {
		//con.execute( "drop table config");
		
		String sql = "create table config ("
				+ "id integer,"
				+ "min_order_size double precision,"
				+ "max_order_size double precision,"
				+ "non_kyc_max_order_size double precision,"
				+ "price_refresh_interval integer,"
				+ "commission double precision,"
				+ "buy_spread double precision,"
				+ "sell_spread double precision,"
				+ "created_at timestamp,"
				+ "updated_at timestamp"
				+ ")";
		con.execute( sql);

		// insert empty row
		con.execute( "insert into config default values");
		
		// update the values
		String sql2 = "update config set "
				+ "min_order_size = 1,"
				+ "max_order_size = 2000,"
				+ "non_kyc_max_order_size = 1000,"
				+ "price_refresh_interval = 20,"
				+ "commission = 1,"
				+ "buy_spread = .005,"
				+ "sell_spread = .005";
		con.execute(sql2);
	}
	
	void createTrades() throws SQLException {
		//con.execute( "drop table trades");
		
		String sql = "create table trades ("
				+ "time varchar(32),"
				+ "orderid varchar(32),"
				+ "side varchar(4),"
				+ "quantity double precision,"
				+ "symbol varchar(32),"
				+ "price double precision,"
				+ "permid varchar(32),"
				+ "cumfill double precision,"
				+ "conid int,"
				+ "exchange varchar(32),"
				+ "avgprice double precision,"
				+ "orderref varchar(256),"
				+ "tradekey varchar(256)"
				+ ")";
		con.execute( sql);
	}
	
	void createOrders() throws SQLException {
		String sql = "create table orders ("
				+ "time timestamptz,"
				+ "wallet varchar(32),"
				+ "cryptoid varchar(32),"
				+ "orderid varchar(32),"
				+ "side varchar(4),"
				+ "quantity double precision,"
				+ "symbol varchar(32),"
				+ "conid int,"
				+ "price double precision,"
				+ "filled int"
				+ ")";
		con.execute( sql);
	}
	
	void insert() throws SQLException {
		con.insert( "people", "peter", 53, "pinecliff");
		S.out( "inserted one");
		
		ResultSet rs = con.query( "SELECT * from people");
		while(rs.next()) {
			S.out( "name=%s  age=%s  address=%s",
					rs.getString(1), rs.getInt(2), rs.getString(3) );
		}		
	}
	
	/** Hitting the remote RefAPI server takes .31 seconds.
	 *  Hitting RefAPI on localhost is .001 seconds. */
	public void testQueryConfigHttp() throws Exception {
		S.out( "Query config from RefAPI server");
		S.out( System.currentTimeMillis() );
		for (int i = 0; i < 100; i++) {
			String data = "{ 'msg': 'getconfig' }";
			HashMap<String, Object> res = TestErrors.sendData(data);
			//S.out( res);
		}
		S.out( System.currentTimeMillis() );
	}

	public void testReadconfigSpreadsheet() {
		try {
			Config config = new Config();
			config.readFromSpreadsheet("Config");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** On my PC it is taking .16 seconds to hit the config table. */
	public void testQueryConfigDb() throws SQLException {
		try {
			con.connect(dbUrl, dbUser, dbPassword);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}

		S.out( "Query database");
		S.out( System.currentTimeMillis() );
		for (int i = 0; i < 1; i++) {
			ResultSet rs = con.query( "SELECT * from config");
			
		}
		S.out( System.currentTimeMillis() );
	}
}
