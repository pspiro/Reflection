package test;

import java.util.Random;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import http.MyAsyncClient;
import tw.util.S;

public class TestMany {
	//static Stocks stocks = new Stocks();
	static JsonArray stocks;
	static Random r = new Random(System.currentTimeMillis());
	static String base = "https://reflection.trading";
	
	public static void main(String[] args) throws Exception {
		Cookie.init();
		synchronized( Cookie.lock) {
			Cookie.lock.wait();
		}
			
		S.out( "got cookie");
		
		MyAsyncClient.get( base + "/api/get-stocks-with-prices", str -> rec( JsonArray.parse(str) ) );
//		Config confg = Config.readFrom("Dev-config");
//		stocks.readFromSheet(confg);
		
	}

	private static void rec(JsonArray ar) {
		stocks = ar;
		//stocks.display();
		
		for (int i = 0; i < 50; i++) {
			new Thread( () -> {
				try {
					sendOrder();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
			S.sleep( 500);
		}
	}
	
	static void sendOrder() {
		JsonObject stock = stocks.get( r.nextInt( stocks.size() ) );

		boolean buy = r.nextBoolean();
		double price = buy ? stock.getDouble("ask") * 1.01 : stock.getDouble("bid") * .99;  
		
		JsonObject obj = new JsonObject();
		obj.put( "action", buy ? "buy" : "sell");
		obj.put( "quantity", 1);
		obj.put( "conid", stock.get("conid") );
		obj.put( "tokenPrice", price);
		obj.put( "wallet_public_key", Cookie.wallet);
		obj.put( "cookie", Cookie.cookie);
		obj.put( "currency", "USDC");
		
		MyAsyncClient.postToJson( base + "/api/order", obj.toString(), json -> json.display() );
		
		
//		String data = TestOrder.orderData( priceOffset, side ? "sell" : "sell", wallet);
		// change this to use BackendOrder
		
//		String data = TestOrder.orderData( priceOffset, side ? "sell" : "sell");
//		MyJsonObject map = TestErrors.sendData( data);
//		synchronized( lock) {
//			S.out( map);
//		}
//		String code = (String)map.get( "code");
//		String text = (String)map.get( "message");		
	}
}
