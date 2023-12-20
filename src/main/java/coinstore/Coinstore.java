package coinstore;

import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Encrypt;
import http.MyClient;

public class Coinstore {
//	private static String m_mdsUrl = String.format( "http://localhost:%s/mdserver/get-ref-prices", m_config.mdsPort() );
//	private static Config m_config;
//	private static Stocks m_stocks;
	static final int pageSize = 100;
	
	static String apiKey = "476644a6179165d624eaa8a170487d0a";
	static String secretKey = "7e5823785f38ed211e97fd9a00874ec7";	
	
	public static void main( String[] args) throws Exception {
		//getPairInfo("BTCUSDT");
		//getPositions().display();
		getTrades("AAPLUSDT").display();
	}
	
	public static JsonArray getTrades(String symbol) throws Exception {
		String params = String.format("symbol=%s&pageNum=%s&pageSize=%s", symbol, 1, pageSize);
		JsonObject obj = get( "/trade/match/accountMatches", params);
		Util.require( obj.getInt("code") == 0, "Coinstore getTrades returned code %s", obj.getInt("code") );
		return obj.getArray("data");
	}
	
	public static JsonArray getPositions() throws Exception {
		String pair = ""; // ignored
		String json = Util.easyJson( "{ 'symbolCodes': [ '%s' ] }", pair);
		JsonObject obj = post( "/spot/accountList", json);
		Util.require( obj.getInt("code") == 0, "Coinstore getPositions returned code %s", obj.getInt("code") );
		return obj.getArray("data");
	}
	
	static void getPairInfo(String pair) throws Exception {
		String json = Util.easyJson( "{ 'symbolCodes': [ '%s' ] }", pair);
		post( "/v2/public/config/spot/symbols", json.toString() );
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
}
		
		
		
		
//		MyServer.listen( m_config.refApiPort(), m_config.threads(), server -> {
//			server.createContext("/path", exch -> new CsTransaction( this, exch).handle() );
//		});
//		
//		m_stocks = m_config.readStocks();
//		for (Stock stock : m_stocks) {
//			// map
//		}
//
//		Util.execute( "CSQ", () -> processQueue() );  // name the thread. pas
//	}
//
//	// probably better to integrate this into RefAPI
//	// ultimately the user orders could be combined with the Coinstore orders--maybe
//	static class CoinStock {
//		String m_contractId;  // lower case
//		int m_conid;
//		String m_symbol;
//		int m_csBalance;
//		int m_ibFilled;
//		int m_ibSub;
//		int m_position;
//		
//		// live orders must be saved so you know how much to subtract out later
//		
//		public boolean shouldBalance() {
//			return Math.abs(m_position) > .5; 
//		}
//		
//		void balance() {
//			Contract contract = new Contract();
//			// set conid and exchange
//
//			Order order = new Order();
//			order.side();
//			order.totalQty( m_desiredQuantity);
//			order.lmtPrice( orderPrice);
//			order.tif( m_config.tif() );  // VERY STRANGE: IOC does not work for API orders in paper system; TWS it works, and DAY works; if we have the same problem in the prod system, we will have to rely on our own timeout mechanism
//			order.allOrNone(session == Session.Smart);  // all or none, we don't want partial fills (not supported for Overnight)
//			order.transmit( true);
//			order.outsideRth( true);
//			order.orderRef(m_uid);
//			
//			
//			
//		}
//	}
//	
//	void incoming(String contractId) {
//		// load up
//		CoinStock cStock = m_map.get(contractId.toLowerCase());
//		// add to the balances
//		
//		
//	}
//	
//	private static Object processQueue() {
//		while (true) {
//			process();
//			S.sleep(60000);
//		}
//	}
//
//	
//	private static void process() {
//		for (CoinStock stk : m_map) {
//			stk.balance();
//		}
//	}
//	
//	
//
//	public void handleOrder( Order order) {
//		
//	}
//
//	public void queryAllPrices() {  // might want to move this into a separate microservice
//		try {
//			MyClient.getArray( m_mdsUrl).forEach( prices -> {
//				Stock stock = m_stocks.getStock( prices.getInt("conid") );
//				if (stock != null) {
//					stock.setPrices( new Prices(prices) );
//				
//					// we never delete a valid last price
//					double last = prices.getDouble("last");
//					if (last > 0) {
//						stock.put( "last", last); // I think it's wrong and Frontend doesn't use this pas
//					}
//				}
//			});
//		}
//		catch( Exception e) {
//			S.out( "Error fetching prices - " + e.getMessage() ); // need this because the exception doesn't give much info
//			// e.printStackTrace(); the stack trace is useless here and fills up the log
//			log( LogType.ERROR_4, e.getMessage() );
//		}
//	}
//	
//
//}
