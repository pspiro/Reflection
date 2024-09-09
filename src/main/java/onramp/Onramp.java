package onramp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JsonObject;

import common.Util;
import http.ClientException;
import http.MyClient;
import tw.util.S;
import web3.Encrypt;

public class Onramp {
	static String apiKey = "WrvBzqWp1QSgXijTi94qJX2YknOv2Y";
	static String secretKey = "a9j1JxuRmJPJ8kVpdBd8WNJ3u8J260Ls";
	static String appId = "950410";  // used by Frontend only

	private static JsonObject tokenMap = new JsonObject();
	
	/*	
	 *  ORDER STATUS CODES
-101  our own code for invalid order id 
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
//		buildMap();
//
//		JsonObject prices = getPrices();
//		prices.getObject( "data").getObject( "onramp").forEach( (key,val) -> 
//			S.out( "%s: %s", tokenMap.getString( key.toString() ), val) );
//		coinLimits(1).display();
		//getQuote().display();
		queryOrderStatus(33);
		
	}

	static String wl = "https://api-test.onramp.money/onramp/api/v2/whiteLabel/onramp";
	static String kyc = "https://api-test.onramp.money/onramp/api/v2/whiteLabel/kyconramp";
	
	
	public static void onramp(String refWalletAddr, double fromAmt) throws Exception {
		double rate = getQuote().getDouble( "rate");

		var resp = whiteLab( "/onramp/createTransaction", Util.toJson( 
				"depositAddress", refWalletAddr,
				"customerId", "custid",  // Unique id received from /kyc/url
				"fromAmount", fromAmt,
				"toAmount", fromAmt * rate,
				"rate", rate
				) );
		resp.display();
	}
	
	private static JsonObject getQuote() throws Exception {
		return whiteLab( "/onramp/quote", Util.toJson( 
				"fromCurrency", "INR",
				"toCurrency", "USDT",
				"fromAmount", "1.",
				"chain", "MATIC20",
				"paymentMethodType", "UPI"
				) );
	}
	
	
	public static void getTransaction() throws Exception {
		var resp = whiteLab( "/onramp/transaction", Util.toJson(
				"transactionId", "",
				"customerId", ""
				));
		resp.display();
	}
	
	public static void getUserTransactions( String custId) throws Exception {
		var resp = whiteLab( "/onramp/allUserTransaction", Util.toJson(
				"page", 1,
				"customerId", custId,
				"pageSize", "500") );
		resp.display();
	}
		
	// used by Monitor?
	public static void getAllTransactions( String custId) throws Exception {
		var resp = whiteLab( "/onramp/allTransaction", Util.toJson(
				"page", 1,
				"pageSize", "500") );
		resp.display();
	}
		
//		
//		{
//		    "status": 1,
//		    "code": 200,
//		    "data": {
//		        "transactionId": "635",
//		        "createdAt": "2023-12-08 00:31:44",
//		        "fiatAmount": "1741",
//		        "fiatPaymentInstructions": {
//		            "type": "IMPS",
//		            "bank": {
//		                "name": "",
//		                "accountNumber": "",
//		                "ifsc": "",
//		                "type": "",
//		                "branch": "",
//		                "bank": ""
//		            },
//		            "bankNotes": [
//		                {
//		                    "type": -1,
//		                    "msg": "NEFT not allowed for this account."
//		                },
//		                {
//		                    "type": 1,
//		                    "msg": "Bank transfers from UPI Apps like gPay, PhonePe, Paytm are working."
//		                },
//		                {
//		                    "type": 1,
//		                    "msg": "UPI bank transfers, IMPS is allowed for this account."
//		                }
//		            ],
//		            "otp": "3217"
//		        }
//		    }
//		}

	
	private static JsonObject whiteLab(String uri, JsonObject bodyJson) throws Exception {
		Util.require( uri.startsWith( "/"), "start with /");
		String url = wl + uri;
		
		String body = bodyJson.toString();

		JsonObject payload = Util.toJson( 
				"timestamp", System.currentTimeMillis(),
				"body", body);

		String encodedPayload = Encrypt.encode( payload.toString() ); 

		// create the signature
		SecretKeySpec keySpec   = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");  // Create HMAC SHA256 key from secret
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(keySpec);
		byte[] result = mac.doFinal( encodedPayload.getBytes() );
		String signature = Encrypt.bytesToHex(result);

		S.out( "body: " + body.toString() );
		S.out( "payload: " + payload);
		S.out( "encoded payload: " + encodedPayload);
		S.out( "signature: " + signature);

		String str = MyClient.create(url, body.toString() )
//				.header("Accept", "application/json")
//				.header("Content-Type", "application/json;charset=UTF-8")
				.header("apikey", apiKey)
				.header("payload", encodedPayload)
				.header("signature", signature)
				.query().body();

		return JsonObject.parse( str);
	}
	
	private static JsonObject getKycUrl() throws Exception {
		return whiteLab( "kyc/url",  Util.toJson(
				"customerId", "abc",
				"email", "abc",
				"phoneNumber*", "123",
				"clientCustomerId*", "abc",
				"type", "INDIVIDUAL",  		// individual or business
				"kycRedirectUrl", "https://reflection.trading",  // user is redirected here after kyc
				"closeAfterLogin", true
				) );
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

	public static boolean isFinal(int status) {
		return status < 0 || status == 4 || status == 15;
	}

	/** Wait up to n seconds for order status. If there's an exception,
	 *  keep waiting */
	public static int waitForOrderStatus(int orderId, int sec) {
		int status = Onramp.queryOrderStatus( orderId);
		for (int i = 0; i < sec && !Onramp.isFinal( status); i++) {
			S.sleep(1000);
			status = Onramp.queryOrderStatus( orderId);
		}
		return status;
	}

	public static int queryOrderStatus(int orderId) {
		try {
			return queryOrder( orderId).getObjectNN( "data").getInt( "orderStatus");
		}
		catch( ClientException e) {
			S.out( "Onramp error for order id %s - %s", orderId, e.getMessage() );
			
			return "Invalid orderId".equals( e.responseJson().getString("error") ) // no need to keep trying in this case
				? -101 : 0;
		}
		catch( Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public static JsonObject queryOrder(int orderId) throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/orderStatus";

		JsonObject query = Util.toJson( 
				"orderId", orderId,
				"type", 1);

		S.out( "querying for order id %s", orderId);
		return query( url, query);
	}

	private static JsonObject query( String url, JsonObject query) throws Exception {
		String body = query.toString();

		JsonObject payload = Util.toJson( 
				"timestamp", System.currentTimeMillis(),
				"body", body);

		String encodedPayload = Encrypt.encode( payload.toString() ); 

		// create the signature
		SecretKeySpec keySpec   = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");  // Create HMAC SHA256 key from secret
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(keySpec);
		byte[] result = mac.doFinal( encodedPayload.getBytes() );
		String signature = Encrypt.bytesToHex(result);

		S.out( "body: " + body.toString() );
		S.out( "payload: " + payload);
		S.out( "encoded payload: " + encodedPayload);
		S.out( "signature: " + signature);

		String str = MyClient.create(url, body)
				.header("Accept", "application/json")
				.header("Content-Type", "application/json;charset=UTF-8")
				.header("X-ONRAMP-APIKEY", apiKey)
				.header("X-ONRAMP-PAYLOAD", encodedPayload)
				.header("X-ONRAMP-SIGNATURE", signature)
				.query().body();

		return JsonObject.parse( str);
	}
}

// questions:
// clientId vs clientCustomerId
// how to call for first time?
// process is: 
// call kycUrl, redirect to url, user kycs,
// when user KYC's is that when they enter their account information as well?
// then how do they change it?
// i need prod and testapi keys
// must I implement a webhook, or can i use a 

/*
 * 
 * 
	 ______________________________________________
	/Convert Fiat to Crypto / Transaction History /
    |                       ---------------------/
	
	<Enter or change account info>
	
	Display redacted account info if we have it
	
 	Enter fiat amount ___________  <Check rate>
 	
 	Rate: _______
 	
 	You will receive __________ RUSD on PulseChain  <GO>
 	
 	
 
  
 
 
*/