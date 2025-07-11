package test;

import org.json.simple.JsonObject;

import common.Util;
import reflection.MySqlConnection;
import tw.util.S;

/** Create trades and commissions tables
 * 
 *  NOTE: According to chart, varchar is as-efficient as varchar(#)
 *  NOTE: if you don't set the timezone here for the created_at, it uses
 *  some other inconsistent timezone; data is always returned in the time
 *  zone that was used when setting the value
 *  
 *  APPARENTLY USING INDEX INSIDE CREATE BLOCK WILL NOT WORK
 *  
 *  
 *  NOTE FIELD NAMES CONVERTED TO AND WILL ALWAYS BE READ BACK IN LOWER CASE!
 *  
 */
public class CreateTables  {
	static MySqlConnection con;
	static final int tradeKeyLen = 64; 
	

	public static void main(String[] args) {
		try {
//			con = ConfigSingleChain.ask().useExternalDbUrl().createConnection();
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
		String sql = "create table commissions ("
			    + "created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),"
				+ "tradekey varchar(32),"
				+ "comm_paid double precision"  // ties in with tradekey from trade
				+ ")";
		con.execute(sql);
	}
	
/*
 signup table

 created_at   | timestamp without time zone |           |          | (CURRENT_TIMESTAMP(6) AT TIME ZONE 'America/New_York'::text)
 email        | character varying(60)       |           |          |
 first        | character varying(60)       |           |          |
 last         | character varying(60)       |           |          |
 referer      | character varying           |           |          |
 country      | character varying(2)        |           |          |
 ip           | character varying(15)       |           |          |
 utm_source   | character varying(200)      |           |          |
 utm_medium   | character varying(200)      |           |          |
 utm_campaign | character varying(200)      |           |          |
 utm_term     | character varying(200)      |           |          |
 utm_content  | character varying(200)      |           |          |
 user_agent   | character varying(400)      |
 actions      | jsonb
 got_price    | boolean
 
 ALTER TABLE signup add column actions jsonb;
 ALTER TABLE signup add column got_prize boolean;
 ALTER TABLE signup drop column id;

 */

	void createLogTable() throws Exception {
		String sql = """
				create table log (
					created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),
					type varchar(32),
					uid varchar(8),
					wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),
					data jsonb, -- see TestLog for how to read this back into a JsonObject

					INDEX log_wallet_key (wallet_public_key)
					);""";		

		con.execute(sql);
	}

	void createOnrampTable() throws Exception {
		String sql = """
				CREATE TABLE onramp (
				    created_at timestamp without time zone DEFAULT (CURRENT_TIMESTAMP(6) AT TIME ZONE 'America/New_York'),
				    wallet_public_key varchar(42) NOT NULL CHECK (wallet_public_key = LOWER(wallet_public_key) AND wallet_public_key <> ''),
				    trans_id varchar(32) NOT NULL UNIQUE CHECK (trans_id <> ''),  -- OnRamp transaction id
				    uid varchar(8),  -- ties back to the transaction submitted by the user
				    fiat_amount double precision NOT NULL,  -- user will pay this
				    crypto_amount double precision NOT NULL,  -- user will receive this
				    state varchar(32),  -- our own status
				    hash varchar(66) -- trans hash of minting RUSD into user's wallet
				);
				
				-- Create the index for wallet_public_key
				CREATE INDEX onramp_wallet_key ON onramp (wallet_public_key);
				""";		

		con.execute(sql);
	}
	
	/** Note that first six are/must be same as transactions table because of the updates from the live order system */
	void createTransactions() throws Exception {
		String sql = """
				create table transactions (
					created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),
					uid varchar(8) primary key, 
					fireblocks_id varchar(36) unique,
					wallet_public_key varchar(42) check (wallet_public_key = LOWER(wallet_public_key)),
					blockchain_hash varchar(66),
					status varchar(32),       -- value from LiveOrderStatus

					symbol varchar(32),
					conid int check (conid > 0),
					action varchar(10),  -- Buy or Sell
					quantity double precision check (quantity > 0),
					rounded_quantity int, -- could be zero
					price double precision check (price > 0),
					order_id int,  -- could be zero
					perm_id int,  -- could be zero
					commission double precision,  -- change this to comm_charged
					tds double precision,				
					currency varchar(32),
					ip_address varchar(32),   -- big enough to store v6 IP format
					country varchar(32)
					ref_code varchar(32),
					
					INDEX trans_wallet_key (wallet_public_key)
				);""";
		con.execute( sql);
	}

	/** Note that first six are/must be same as transactions table because of the updates from the live order system */
	void createRedemptions() throws Exception {
		String sql ="create table redemptions ("
				+ "created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),"
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
		String sql = "create table trades ("  // you could add uid here, but you would have to create a map of orderid or permid to uid or OrderTransaction
			    + "created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),"
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
	
	void createEmail() throws Exception {
		String sql = """
		CREATE TABLE email (
			created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),
			email varchar(100),
			subject varchar(100),
			text text
		);
		""";
		con.execute( sql);
	}
	
	/* locked jsonb fields
	 * 
	 * faucet: {
	 *   <blockchain name>: amount
	 *   }
	 *   
	 * amount: 0.00
	 * lockedUntil: time in ms
	 * requiredTrades: int
	 * rewarded: boolean  true if collected some prize
	 *  
	 */
	
	static record User(
			String created_at,
			String wallet_public_key,
			String first_name,
			String last_name,
			String email,
			String phone,
			String kyc_status,
			String address, 
			String address_1, 
			String address_2, 
			String city, 
			String state ,
			String zip, 
			String country, 
			String geo_code ,
			String telegram, 
			String persona_response, 
			String pan_number, 
			String aadhaar, 
			JsonObject locked,
			String ip, 
			String onramp_id
			) {		
	}
		
	
	/** This has never been run and probably doesn't work */
	void createUsers() throws Exception {
		String sql = """
		CREATE TABLE public.users (
			created_at timestamp without time zone default(CURRENT_TIMESTAMP(6) at time zone 'America/New_York'),
			wallet_public_key varchar(42) PRIMARY check (wallet_public_key = LOWER(wallet_public_key)),
			first_name varchar(50),
			last_name varchar(50),
			email varchar(100),
			phone varchar(20),
			kyc_status varchar(20),
			address varchar(100),
			address_1 varchar(100),
			address_2 varchar(100),
			city varchar(50),
			state varchar(100),
			zip varchar(20),
			country varchar(50),  -- country entered by user
			geo_code varying(2),  -- based on geo-location
			telegram varying(50),
			persona_response varchar,
			pan_number varchar(10),
			aadhaar varchar(12),
			locked jsonb,		-- generic json object
			ip varchar(15),
			onramp_id
		);
		""";
		con.execute( sql);
	}
}

// add an id field to a table (assigns an id to all records)
//dev=> alter table users add column id INT GENERATED ALWAYS AS IDENTITY unique;
//dev=> alter table signup add column id INT GENERATED ALWAYS AS IDENTITY unique;
// dev=> alter table users add column onramp_id varchar(64);

// dev=> alter table users add column locked jsonb;


/*
create chainId fields
ALTER TABLE log ADD COLUMN chainId INT;
ALTER TABLE onramp ADD COLUMN chainId INT;
ALTER TABLE orders ADD COLUMN chainId INT;
ALTER TABLE redemptions ADD COLUMN chainId INT;
ALTER TABLE transactions ADD COLUMN chainId INT;

// do we need these???
CREATE INDEX idx_chainId1 ON log (chainId);
CREATE INDEX idx_chainId2 ON onramp (chainId);
CREATE INDEX idx_chainId3 ON orders (chainId);
CREATE INDEX idx_chainId4 ON redemptions (chainId);
CREATE INDEX idx_chainId5 ON transactions (chainId);

ALTER TABLE log drop COLUMN chain;
ALTER TABLE onramp drop COLUMN chain;
ALTER TABLE orders drop COLUMN chain;
ALTER TABLE redemptions drop COLUMN chain;
ALTER TABLE transactions drop COLUMN chain;
*/

