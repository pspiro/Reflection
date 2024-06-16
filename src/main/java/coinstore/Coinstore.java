package coinstore;

import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.Action;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.Encrypt;

public class Coinstore {
//	private static String m_mdsUrl = String.format( "http://localhost:%s/mdserver/get-ref-prices", m_config.mdsPort() );
//	private static Config m_config;
//	private static Stocks m_stocks;
	static final int maxPageSize = 100;
	
	static String apiKey = "476644a6179165d624eaa8a170487d0a";
	static String secretKey = "7e5823785f38ed211e97fd9a00874ec7";
	static String aapl = "AAPLUSDT";
	
	static double minQty = .005;
	static double maxQty = 10;
	
	static class CsStock {
		String m_symbol;
		double m_minPrice;
		double m_maxPrice;
		
		CsStock( String symbol, double min, double max) {
			m_symbol = symbol;
			m_minPrice = min;
			m_maxPrice = max;
		}
		
		public long placeOrder(Action action, double qty, double price) throws Exception {
			Util.require( isBetween( qty, minQty, maxQty), "quantity %s is out of range", qty); 
			Util.require( isBetween( price, m_minPrice, m_maxPrice), "quantity %s is out of range", qty); 
			
			S.out( "Placing order %s %s %s at %s", action, qty, m_symbol, price);
			JsonObject json = Util.toJson(
					"symbol", m_symbol,
					"side", action.toString().toUpperCase(),
					"orderQty", S.fmt2d( qty),
					"ordType", "LIMIT",
					"ordPrice", S.fmt2d( price), 
					"timestamp", System.currentTimeMillis() );
					
			JsonObject ret = post("/trade/order/place", json);
			S.out( ret);
			return ret.getObject("data").getLong("ordId");
		}
		
		JsonObject cancel(long orderId) throws Exception {
			S.out( "Canceling %s", orderId);
			return post( "/trade/order/cancel", Util.toJson( "symbol", m_symbol, "ordId", orderId) );
		}
		
	}
	
	static class Aapl extends CsStock {
		Aapl() {
			super( aapl, 180, 220);
		}
	}
	
	public static void cancelAll() throws Exception {
		JsonObject obj = get( "/trade/order/cancelAll", "");
		obj.display();
	}
	
	public static void main( String[] args) throws Exception {
		//placeOrder(Action.Buy, 1, aapl, 192);
		//cancelAll(aapl);
		//testOrder();
		//getOpenOrders().print();
		getLatestTrades(aapl).print();
	}
	
	
	public static void cancelAll(String symbol) throws Exception {
		String params = String.format( "symbol=%s", symbol);
		get( "/trade/order/cancelAll", params).display();
	}
	
	static void showTrades() throws Exception {
		
		getLatestTrades(aapl).forEach( trade -> S.out(trade) );
		
		JsonArray ar = getAllTrades(aapl);
		S.out( ar.get(0).keySet() );
		
		double buyAmt = 0;
		double sellAmt = 0;
		double buyQty = 0;
		double sellQty = 0;
		for (JsonObject trade : ar) {
			double qty = trade.getDouble("execQty");
			double amt = trade.getDouble("execAmt");
			if (trade.getString("side").equals("1") ) {
				buyAmt += amt;
				buyQty += qty;
			}
			else {
				sellAmt += amt;
				sellQty += qty;
			}
		}
		S.out( "trades=%s buyAmt=%s sellAmt=%s buyQty=%s sellQty=%s avgBuy=%s avgSell=%s",
				ar.size(), buyAmt, sellAmt, buyQty, sellQty, buyAmt / buyQty, sellAmt / sellQty);
		
	}
	
	public static JsonArray getOpenOrders() throws Exception {
		String params = String.format("symbol=%s", aapl);
		JsonObject obj = get( "/v2/trade/order/active", params);
		Util.require( obj.getInt("code") == 0, "Coinstore getTrades returned code %s", obj.getInt("code") );

		JsonArray ar = obj.getArray("data");
		ar.convertToDouble( "leavesQty");
		ar.convertToDouble( "ordPrice");
		
		return ar;
	}
	
	public static JsonArray getLatestTrades(String symbol) throws Exception {
		//String params = String.format("symbol=%s&pageNum=%s&pageSize=%s", symbol, 1, maxPageSize);
		String params = String.format("symbol=%s", symbol);
		JsonObject obj = get( "/trade/match/accountMatches", params);
		Util.require( obj.getInt("code") == 0, "Coinstore getTrades returned code %s", obj.getInt("code") );
		return obj.getArray("data");
	}
	
	public static JsonArray getAllTrades(String symbol) throws Exception {
		JsonArray ar = new JsonArray();
		int page = 1;
		
		while (true) {
			String params = String.format("symbol=%s&pageNum=%s&pageSize=%s", symbol, page, maxPageSize);
			JsonObject obj = get( "/trade/match/accountMatches", params);
			Util.require( obj.getInt("code") == 0, "Coinstore getTrades returned code %s", obj.getInt("code") );
			JsonArray ret = obj.getArray("data");
			ar.addAll( ret);
			if (ret.size() < maxPageSize) {
				break;
			}
			S.sleep(10);
			page++;
		}
		return ar;
	}
	
	/** fields are uid,accountId,currency,balance,typeName, maybe others */
	public static JsonArray getPositions() throws Exception {
		String pair = ""; // ignored
		String json = Util.easyJson( "{ 'symbolCodes': [ '%s' ] }", pair);
		JsonObject obj = post( "/spot/accountList", json);
		Util.require( obj.getInt("code") == 0, "Coinstore getPositions returned code %s", obj.getInt("code") );
		return obj.getArray("data");
	}
	
	static void getPairInfo(String pair) throws Exception {
		String json = Util.easyJson( "{ 'symbolCodes': [ '%s' ] }", pair);
		post( "/v2/public/config/spot/symbols", json);
	}
	
	// combine get and post
	static JsonObject get(String path, String params) throws Exception {
		long cur = System.currentTimeMillis();
		String expires = "" + cur / 30000;

		String nextKey = sign(secretKey, expires);
		
		String signed = sign( nextKey, params);

		HttpResponse<String> resp = MyClient
			.create( String.format( "https://api.coinstore.com/api%s?%s", path, params) )
			.header( "X-CS-APIKEY", apiKey)
			.header( "X-CS-EXPIRES", "" + cur)
			.header( "X-CS-SIGN", signed)
			.header( "Content-Type", "application/json")
			.query();

		Util.require( resp.statusCode() == 200, "Coinstore query response code %s", resp.statusCode() );
		
		return JsonObject.parse( resp.body() );
	}
	
	// combine get and post
	static JsonObject post( String path, JsonObject json) throws Exception {
		return post( path, json.toString() );
	}
	
	static JsonObject post( String path, String json) throws Exception {
		long cur = System.currentTimeMillis();
		String expires = "" + cur / 30000;

		String nextKey = sign(secretKey, expires);
		
		String signed = sign( nextKey, json);
		
		HttpResponse<String> resp = MyClient
			.create( "https://api.coinstore.com/api" + path, json)
			.header( "X-CS-APIKEY", apiKey)
			.header( "X-CS-EXPIRES", "" + cur)
			.header( "X-CS-SIGN", signed)
			.header( "Content-Type", "application/json")
			.query();

		Util.require( resp.statusCode() == 200, "Coinstore query response code %s", resp.statusCode() );

		return JsonObject.parse( resp.body() );
	}
	
	public static String sign(String key, String data) {
	    try {
	        // Create HMAC SHA256 key from secret
	        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");

	        // Get Mac instance and initialize with the HMAC SHA256 key
	        Mac mac = Mac.getInstance("HmacSHA256");
	        mac.init(keySpec);

	        // Perform HMAC SHA256 and get the result
	        byte[] result = mac.doFinal(data.getBytes());

	        // Convert the result to a Hex String
	        String ret = Encrypt.bytesToHex(result);
	        
	        // debug
//	        S.out( "key: %s", key);
//	        S.out( "data: %s", data);
//	        S.out( "encrypted: ** %s **", ret);
//	        S.out();
	        
	        return ret;
	    } 
	    catch (NoSuchAlgorithmException | InvalidKeyException e) {
	        throw new RuntimeException("Failed to calculate HMAC SHA256", e);
	    }
	}

	static boolean isBetween(double price, double low, double high) {
		return price >= low && price <= high;
	}
}
