package test;

import org.json.simple.JsonArray;

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

//			new CreateTables().createCryptoTransactions();
//			//new CreateTables().createTrades();
			new CreateTables().createCommTable();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		S.out( "done");
	}
	
	void createCommTable() throws Exception {
		con.dropTable("commissions");
		
		String sql = "create table commissions ("
				+ "tradekey varchar(32),"
				+ "comm_paid double precision"
				+ "currency varchar(3)"
				+ ")";
		con.execute(sql);
	}
	
	void createCryptoTransactions() throws Exception {
		con.dropTable("crypto_transactions");
		
		String sql = "create table crypto_transactions ("   // in Java 13 you have text blocks, you wouldn't need all the + "
				+ "fireblocks_id varchar(36) check (fireblocks_id <> ''),"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "symbol varchar(32),"
				+ "conid int check (conid > 0),"
				+ "action varchar(10),"
				+ "quantity double precision check (quantity > 0),"
				+ "rounded_quantity int," // could be zero
				+ "price double precision check (price > 0),"
				+ "order_id int,"  // could be zero
				+ "perm_id int,"  // could be zero
				+ "timestamp bigint,"   // eight bytes, signed
				+ "blockchain_hash varchar(66),"
				+ "commission double precision,"  // change this to comm_charged
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
		
		String sql = "create table commissions ("
				+ "tradekey varchar(32),"			// tie commission report to trade
				+ "commission double precision,"
				+ "currency varchar(3)"
				+ ")";
		con.execute( sql);
	}
	
	void createTrades() throws Exception {
		con.dropTable( "trades");
		
		String sql = "create table trades ("
				+ "tradekey varchar(32),"  // tie the trade to the commission report
				+ "order_id int,"
				+ "perm_id int check (perm_id <> 0),"  // can't be zero because then we can't tie it to the crypto_transaction
				+ "time varchar(32),"
				+ "side varchar(4),"
				+ "quantity double precision,"
				+ "symbol varchar(32),"
				+ "price double precision,"
				+ "cumfill double precision,"
				+ "conid int,"
				+ "exchange varchar(32),"
				+ "avgprice double precision,"
				+ "orderref varchar(256)"
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
