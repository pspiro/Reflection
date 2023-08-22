package test;

import reflection.MySqlConnection;
import tw.util.S;

/** Create trades and commissions tables */
public class CreateTables  {
	static String dbUrl = "jdbc:postgresql://34.86.193.58:5432/reflection";
	static String dbUser = "postgres";
	static String dbPassword = "1359";
	static MySqlConnection con = new MySqlConnection();
	static final int tradeKeyLen = 64; 

	public static void main(String[] args) {
		try {
			con.connect(dbUrl, dbUser, dbPassword);

			new CreateTables().createCryptoTransactions();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		S.out( "done");
	}
	
	void createCryptoTransactions() throws Exception {
		con.dropTable("crypto_transactions");
		
		String sql = "create table crypto_transactions ("   // in Java 13 you have text blocks, you wouldn't need all the + "
				+ "order_id varchar(32),"
				+ "perm_id varchar(32),"
				+ "fireblocks_id varchar(36),"
				+ "timestamp bigint,"   // eight bytes, signed
				+ "blockchain_hash varchar(66),"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "symbol varchar(32),"
				+ "conid int,"
				+ "action varchar(10),"
				+ "quantity double precision,"
				+ "price double precision,"
				+ "commission double precision,"
				+ "tds double precision,"				
				+ "currency varchar(32),"
				+ "status varchar(32),"       // value from FireblocksStatus
				+ "ip_address varchar(32),"   // big enough to store v6 IP format
				+ "city varchar(32),"
				+ "country varchar(32)"
				+ ")";
		con.execute( sql);
	}
	
	void createCommissions() throws Exception {
		con.dropTable("commissions");
		
		String sql = "create table commissions ("   // in Java 13 you have text blocks, you wouldn't need all the + "
				+ "tradekey varchar(32),"
				+ "commission double precision,"
				+ "currency varchar(3)"
				+ ")";
		con.execute( sql);
	}
	
	void createTrades() throws Exception {
		con.dropTable( "trades");
		
		String sql = "create table trades ("
				+ "order_id varchar(32),"
				+ "perm_id varchar(32),"
				+ "time varchar(32),"
				+ "side varchar(4),"
				+ "quantity double precision,"
				+ "symbol varchar(32),"
				+ "price double precision,"
				+ "cumfill double precision,"
				+ "conid int,"
				+ "exchange varchar(32),"
				+ "avgprice double precision,"
				+ "orderref varchar(256),"
				+ "tradekey varchar(32)"
				+ ")";
		con.execute( sql);
	}
	
	void createUsers() throws Exception {
		con.dropTable( "trades");

		// to add unique to an existing table:
		// ALTER TABLE users ADD UNIQUE (wallet_public_key);
		
		// fields:
//		id
//		name
//		email
//		phone
//		wallet_public_key  // must be UNIQUE and lower case, e.g.:
//		wallet_public_key varchar(42) unique CHECK (lowercase_column = LOWER(lowercase_column))
//		kyc_status  // should remove this and check 
//		address
//		active
//		is_black_listed
//		created_at
//		updated_at
//		city
//		country
//		persona_response
//		pan_number
//		aadhaar
		
		String sql = "create table users ("
				// write this
				+ ")";
		con.execute( sql);
	}
}
