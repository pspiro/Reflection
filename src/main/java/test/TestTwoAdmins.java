package test;

import java.util.Random;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.S;

/** This test proves that more admins are quicker. One admin 
 *  takes 50% longer to run than two admins. */
public class TestTwoAdmins {
	static final String base = "https://reflection.trading";

	//static Stocks stocks = new Stocks();
	static JsonArray stockPrices;
	static Random r = new Random(System.currentTimeMillis());

	static String[] addrs = {
			"0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3",
			"0xb95bf9C71e030FA3D8c0940456972885DB60843F",
	};
	
	public static void main(String[] args) throws Throwable {
		//seed();
		//System.exit(0);

		String data = MyClient.getString( base + "/api/get-stocks-with-prices");
		stockPrices = JsonArray.parse(data);

		for (String addr : addrs) {
			Util.execute( () -> new Wal().init(addr) );
			S.sleep(50);
		}
	}

	static class Wal {
		Cookie2 cook = new Cookie2(base);

		void init(String ad) {
			Util.wrap( () -> {
				cook.signIn(ad);

				for (int i = 0; i < 3; i++) {
					sendOrder();
					S.sleep( 1000);
				}
			});
		}
		
		void sendOrder() throws Exception {
			// pick random stock, buy or sell, qty 1
			
			JsonObject stock = stockPrices.get( 0);

			double price = stock.getDouble("ask") * 1.05;
			
			if (price <= 0) {
				S.out( "ERROR PRICE IS %s for %s", price, stock.getString("symbol") );
				return;
			}
			
			S.out( "submitting with price %s", price);
			
			JsonObject obj = new JsonObject();
			obj.put( "action", "buy");
			obj.put( "quantity", 1);
			obj.put( "conid", stock.get("conid") );
			obj.put( "tokenPrice", price);
			obj.put( "currency", "RUSD");
			obj.put( "wallet_public_key", cook.address() );
			obj.put( "cookie", cook.cookie() );
			
			S.out( "Submitting " + obj);
			MyClient.postToJson( base + "/api/order", obj.toString() ).display();
		}
		
	}
	
	static void seed() throws Exception {
		S.out( "Seeding");

		// create config and pass in
		
		for (String wallet : addrs) {
			mint( wallet, r.nextInt(5000, 100000), null); 
			//config.busd().mint(addr, r.nextInt(5000, 100000) );  // if you use BUSD, you have to approve it first
			S.out( "minted");
			//createUserProfile(wallet.toLowerCase(), config);
		}
		S.out( "done");
		System.exit(0);
	}
	
	public static void mint(String wallet, double amt, Config config) throws Exception {

		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		config.rusd().mintRusd( wallet, amt, stocks.getAnyStockToken() )
			.waitForCompleted();
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
