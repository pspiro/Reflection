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
				+ "comm_paid double precision"  // ties in with tradekey from trade
				+ ")";
		con.execute(sql);
	}
	
	void createSignupTable() throws Exception {
		con.dropTable("signup");
		
		String sql = "create table signup ("
			    + "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "name varchar(60),"
				+ "email varchar(60),"
				+ "phone varchar(20)"
				+ ")";
		con.execute(sql);
	}

	void createLogTable() throws Exception {
		con.dropTable("log");
		
		String sql = "create table log ("
			    + "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
				+ "type varchar(32),"
			    + "uid varchar(8),"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
			    + "data jsonb"  // see TestLog for how to read this back into a JsonObject

				+ ")";
		con.execute(sql);
	}
	
	void createTransactions() throws Exception {
		con.dropTable("transactions");
		
		String sql =""
				+ "create table transactions ("
				+ "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
				+ "uid varchar(8) primary key," 
				+ "fireblocks_id varchar(36) unique,"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "symbol varchar(32),"
				+ "conid int check (conid > 0),"
				+ "action varchar(10),"
				+ "quantity double precision check (quantity > 0),"
				+ "rounded_quantity int," // could be zero
				+ "price double precision check (price > 0),"
				+ "order_id int,"  // could be zero
				+ "perm_id int,"  // could be zero
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
	
	void createTrades() throws Exception {
		con.dropTable( "trades");
		
		String sql = "create table trades ("  // you could add uid here, but you would have to create a map of orderid or permid to uid or OrderTransaction
			    + "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
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
	
	/** This has never been run and probably doesn't work */
	void createUsers() throws Exception {
//		con.dropTable( "users");		
//		String sql = """
//			CREATE TABLE users (
//			created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),
//			updated_at timestamp without time zone,
//			wallet_public_key varchar(42) UNIQUE check (wallet_public_key = LOWER(wallet_public_key)),
		                                 // OR PRIMARY, if you want to prevent null wallets
//			first_name varchar(50),
//			last_name varchar(50),
//			email varchar,
//			phone varchar,
//			kyc_status varchar,
//			address varchar,
//			city varchar,
//			country varchar,
//			persona_response varchar,
//			pan_number varchar(10),
//			aadhaar varchar(12)
//		""";		

//		String sql = "create table users ("
//				// write this
//				+ ")";
//		con.execute( sql);
	}
}
