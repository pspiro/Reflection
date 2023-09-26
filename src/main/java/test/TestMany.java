package test;

import java.util.Random;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.MintRusd;
import http.MyAsyncClient;
import reflection.Config;
import tw.util.S;

public class TestMany {
	//static Stocks stocks = new Stocks();
	static JsonArray stocks;
	static Random r = new Random(System.currentTimeMillis());
	static String base = "https://reflection.trading";
	//static String base = "http://localhost:8383";
	
	static String[] addrs = {
			"0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3",
			"0xb95bf9C71e030FA3D8c0940456972885DB60843F",
			"0x76274e9a0F0bc4EB9389e013bD00b2c4303cDd37",
			"0xbb0E3B579e9A7D9f706b36f5F137C20B0A7aCC3D",
	};
	
	int index = 0;
	
	public static void main(String[] args) throws Exception {
		seed();
		if(true) throw new Exception();
		
		String data = MyAsyncClient.get( base + "/api/get-stocks-with-prices");
		stocks = JsonArray.parse(data);

		for (String addr : addrs) {
			Wal wal = new Wal();
			Util.execute( () -> wal.init(addr) );
		}
	}
	
	static void seed() throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		for (String addr : addrs) {
			MintRusd.mint(addr, r.nextInt(5000, 100000) );
			config.busd().mint(addr, r.nextInt(5000, 100000) );
		}
		
	}

	static class Wal {
		Cookie2 cook = new Cookie2();

		void init(String ad) {
			cook.signIn(ad, () -> placeOrders() );
		}

		private void placeOrders() {
			for (int i = 0; i < 10; i++) {
				sendOrder();
				S.sleep( 1000);
			}
		}
		
		void sendOrder() {
			// pick random stock, buy or sell, qty 1
			
			JsonObject stock = stocks.get( r.nextInt( stocks.size() ) );

			boolean buy = r.nextBoolean();
			double price = buy ? stock.getDouble("ask") * 1.01 : stock.getDouble("bid") * .99;
			String currency = r.nextBoolean() ? "RUSD" : "BUSD";
			
			JsonObject obj = new JsonObject();
			obj.put( "action", buy ? "buy" : "sell");
			obj.put( "quantity", 1);
			obj.put( "conid", stock.get("conid") );
			obj.put( "tokenPrice", price);
			obj.put( "currency", "RUSD");
			obj.put( "wallet_public_key", cook.address() );
			obj.put( "cookie", cook.cookie() );
			
			MyAsyncClient.postToJson( base + "/api/order", obj.toString(), json -> json.display() );
		}
		
	}

	
}
