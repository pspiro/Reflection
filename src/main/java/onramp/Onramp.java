package onramp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Encrypt;
import http.MyClient;
import tw.util.S;

public class Onramp {
	static String apiKey = "WrvBzqWp1QSgXijTi94qJX2YknOv2Y";
	static String secretKey = "a9j1JxuRmJPJ8kVpdBd8WNJ3u8J260Ls";
	static String appId = "9504102";  // used by Frontend only

	private static JsonObject tokenMap = new JsonObject();
	
	/*	
	 *  ORDER STATUS CODES 
-4  amount mismatch  The sent amount does not match the required amount.  
-3  bank and kyc name mismatch  The names on the bank account and KYC do not match.  
-2  transaction abandoned  The user has abandoned the transaction.  
-1  transaction timed out  The transaction has exceeded the allowable time limit.  
0  transaction created  The transaction has been successfully created.  
1  referenceId claimed  The Reference ID has been successfully claimed.  
2  deposit secured  The deposit for the transaction has been secured.  
3, 13  crypto purchased  The desired cryptocurrency has been purchased.  
4, 15  withdrawal complete  The withdrawal process is completed.  
5, 16  webhook sent  The webhook notification has been sent.  
11  order placement initiated  The process of placing the order has begun.  
12  purchasing crypto  The cryptocurrency purchase is in progress.  
14  withdrawal initiated  The withdrawal process has started.
	 */

	public static void main(String[] args) throws Exception {
//		queryHistory();
//		queryLimits().display();
//		coinLimits(1).display();
		buildMap();

		JsonObject prices = getPrices();
		prices.getObject( "data").getObject( "onramp").forEach( (key,val) -> 
			S.out( "%s: %s", tokenMap.getString( key.toString() ), val) );
	}

	public static void buildMap() throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/allConfigMapping";
		JsonObject json = query( url, new JsonObject() );
		json.getObject( "data").getObject( "coinSymbolMapping").forEach( (key,val) -> tokenMap.put( val.toString(), key) );
	}

	public static JsonObject queryLimits() throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/limits";
		return query( url, Util.toJson( "type", 1) );  // 1=onramp, 2=offramp, 3=both
	}

	public static JsonObject getPrices() throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/priceTicker";
		return query( url, Util.toJson( 
				"type", 1,         // 1=buy  2=sell  3=both
				"fiatType", 1) );  // 1=INR
	}

	/** Returns a map of coinid -> chainid -> limit */
	public static JsonObject coinLimits(int fiatType) throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/allCoinLimits";
		return query( url, Util.toJson( "fiatType", fiatType) );  // 1=onramp, 2=offramp, 3=both
	}

	//	public static void query() {
	//	}
	//	public static void query() {
	//	}
	//	public static void query() {
	//	}
	//	public static void query() {
	//	}

	public static void queryFees() throws Exception {
		String url = "https://api.onramp.money/onramp/api/v1/public/allGasFee";
		S.out( "querying for fees");
		MyClient.getJson( url).display();
	}
	public static void queryHistory() throws Exception {

		String url = "https://api.onramp.money/onramp/api/v1/transaction/merchantHistory";

		JsonObject query = Util.toJson(
				"page", 1,
				"pageSize", 50   // Min: 1, Max: 500, Default: 50
				//				"since", "2022-10-07T22:29:52.000Z"
				);
		S.out( "querying for history");
		query( url, query).display();
	}

	public static void queryOrderId(int orderId) throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/orderStatus";

		JsonObject query = Util.toJson( 
				"orderId", orderId,
				"type", 1);

		S.out( "querying for order id %s", orderId);
		query( url, query).display();
	}

	private static JsonObject query( String url, JsonObject query) throws Exception {
		String body = query.toString();

		JsonObject payload = Util.toJson( 
				"timestamp", System.currentTimeMillis(),
				"body", body);

		String encodedPayload = Encrypt.encode( payload.toString() ); 

		SecretKeySpec keySpec   = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");  // Create HMAC SHA256 key from secret

		// create the signature
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(keySpec);
		byte[] result = mac.doFinal( encodedPayload.getBytes() );
		String signature = Encrypt.bytesToHex(result);

		S.out( "body: " + body.toString() );
		S.out( "payload: " + payload);
		S.out( "encoded payload: " + encodedPayload);
		S.out( "signature: " + signature);

		String str = MyClient.create(url, body.toString() )
				.header("Accept", "application/json")
				.header("Content-Type", "application/json;charset=UTF-8")
				.header("X-ONRAMP-APIKEY", apiKey)
				.header("X-ONRAMP-PAYLOAD", encodedPayload)
				.header("X-ONRAMP-SIGNATURE", signature).query().body();

		return JsonObject.parse( str);
	}
}
