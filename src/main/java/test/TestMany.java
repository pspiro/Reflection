package test;

import java.util.Random;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import reflection.Config;
import tw.util.S;


/*
 * 
 * 
 * REMEMBER, YOU WILL GET TIMEOUTS BECAUSE YOU CANNOT USE IOC IN PAPER SYSTEM
 * 
 * 
 */
public class TestMany {
	static final String base = "https://reflection.trading";
	//static String base = "http://localhost:8383";

	//static Stocks stocks = new Stocks();
	static JsonArray stockPrices;
	static Random r = new Random(System.currentTimeMillis());

	//static Stocks stocks = new Stocks();
	
	static String[] addrs = {
			"0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3",
			"0xb95bf9C71e030FA3D8c0940456972885DB60843F",
			"0x76274e9a0F0bc4EB9389e013bD00b2c4303cDd37",
			"0xbb0E3B579e9A7D9f706b36f5F137C20B0A7aCC3D",
	};
	
	int index = 0;
	static int count = 10;
	
	public static void main(String[] args) throws Throwable {
		//seed();

		String data = MyClient.getString( base + "/api/get-stocks-with-prices");
		stockPrices = JsonArray.parse(data);

		for (String addr : addrs) {
			Wal wal = new Wal();
			Util.execute( () -> wal.init(addr) );
			if (count == 1) break;
			S.sleep(50);
		}
	}

	static class Wal {
		Cookie2 cook = new Cookie2(base);

		void init(String ad) {
			Util.wrap( () -> {
				cook.signIn(ad);
				
				for (int i = 0; i < count; i++) {
					sendOrder();
					S.sleep( 2000);
				}
			});
		}
		
		void sendOrder() {
			// pick random stock, buy or sell, qty 1
			
			JsonObject stock = stockPrices.get( r.nextInt( stockPrices.size() ) );

			boolean buy = r.nextBoolean();
			double price = buy ? stock.getDouble("ask") * 1.01 : stock.getDouble("bid") * .99;
			
			if (price <= 0) {
				S.out( "ERROR PRICE IS %s for %s", price, stock.getString("symbol") );
				return;
			}
			//String currency = r.nextBoolean() ? "RUSD" : "BUSD";
			
			JsonObject obj = new JsonObject();
			obj.put( "action", buy ? "buy" : "sell");
			obj.put( "quantity", 1);
			obj.put( "conid", stock.get("conid") );
			obj.put( "tokenPrice", price);
			obj.put( "currency", "RUSD");
			obj.put( "wallet_public_key", cook.address() );
			obj.put( "cookie", cook.cookie() );
			
			MyClient.postToJson( base + "/api/order", obj.toString(), json -> S.out( json) );
			S.out( "Submitted " + obj);
		}
		
	}
	
	static void seed() throws Exception {
		S.out( "Seeding");

		// create config and pass in
		
		for (String wallet : addrs) {
			TestTwoAdmins.mint( wallet, r.nextInt(5000, 100000), null); 
			//config.busd().mint(addr, r.nextInt(5000, 100000) );  // if you use BUSD, you have to approve it first
			S.out( "minted");
			//createUserProfile(wallet.toLowerCase(), config);
		}
		S.out( "done");
		System.exit(0);
	}

	private static void createUserProfile(String wallet, Config config) throws Exception {
		JsonObject o = new JsonObject();
		o.put( "wallet_public_key", wallet);
		o.put( "first_name", "peter");
		o.put( "last_name", "spiro");
		o.put( "email", "peteraspiro@gmail.com");
		o.put( "phone", "9143933732");
		o.put( "address", "Pinecliff Rd");
		o.put( "pan_number", "8383838383");
		o.put( "aadhaar", "838383838383");
		config.sqlCommand(sql -> sql.insertOrUpdate("users", o, "wallet_public_key = '%s'", wallet) );
		S.out( "Created user profile for %s", wallet);
	}

	
}
