package test;

import common.Util;
import reflection.Config;
import reflection.MySqlConnection;
import tw.util.S;

/** Create trades and commissions tables
 * 
 *  NOTE: According to chart, varchar is as-efficient as varchar(#)
 */
public class CreateTables  {
	static MySqlConnection con;
	static final int tradeKeyLen = 64; 
	

	public static void main(String[] args) {
		try {
			con = Config.ask().useExternalDbUrl().createConnection();
			new CreateTables().createRedemptions();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (con != null) {
				Util.wrap( () -> con.close() );
			}
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
	
	/** Note that first six are/must be same as redemptions table */
	void createTransactions() throws Exception {
		con.dropTable("transactions");
		
		String sql =""
				+ "create table transactions ("
				
				+ "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
				+ "uid varchar(8) primary key," 
				+ "fireblocks_id varchar(36) unique,"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "blockchain_hash varchar(66),"
				+ "status varchar(32),"       // value from LiveOrderStatus
				
				+ "symbol varchar(32),"
				+ "conid int check (conid > 0),"
				+ "action varchar(10),"
				+ "quantity double precision check (quantity > 0),"
				+ "rounded_quantity int," // could be zero
				+ "price double precision check (price > 0),"
				+ "order_id int,"  // could be zero
				+ "perm_id int,"  // could be zero
				+ "commission double precision,"  // change this to comm_charged
				+ "tds double precision,"				
				+ "currency varchar(32),"
				+ "ip_address varchar(32),"   // big enough to store v6 IP format
				+ "city varchar(32),"
				+ "country varchar(32)"
				+ ")";
		con.execute( sql);
	}

	/** Note that first six are/must be same as transactions table */
	void createRedemptions() throws Exception {
		con.dropTable("redemptions");
		
		String sql =""
				+ "create table redemptions ("
				
				+ "created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),"
				+ "uid varchar(8) primary key," 
				+ "fireblocks_id varchar(36) unique,"
				+ "wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),"
				+ "blockchain_hash varchar(66),"
				+ "status varchar(32),"       // value from RedeemTransaction.LiveStatus
				+ "stablecoin varchar(6),"
				+ "amount double precision"
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
		con.dropTable( "users");
		
		String sql = """
		CREATE TABLE public.users (
			created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP(6),
			wallet_public_key varchar(42) PRIMARY check (wallet_public_key = LOWER(wallet_public_key)),
			first_name character varying(50),
			last_name character varying(50),
			email character varying,
			phone character varying,
			kyc_status character varying,
			address character varying,
			city character varying,
			country character varying,
			persona_response character varying,
			pan_number character varying(10),
			aadhaar character varying(12)
		);
		""";
		con.execute( sql);
	}
}
