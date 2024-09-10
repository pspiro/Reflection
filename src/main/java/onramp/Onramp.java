package onramp;

import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JsonObject;

import common.Util;
import http.ClientException;
import http.MyClient;
import tw.util.MyException;
import tw.util.S;
import web3.Encrypt;

public class Onramp {
	static String apiKey = "WrvBzqWp1QSgXijTi94qJX2YknOv2Y";
	static String secretKey = "a9j1JxuRmJPJ8kVpdBd8WNJ3u8J260Ls";
	static String appId = "950410";  // used by Frontend only

//	static JsonObject mapIdToFiat = new JsonObject();
	//static JsonObject mapFiatToId; // not used
	static HashMap<String,Double> mapFiatToRate = new HashMap<>();
	static JsonObject mapFiatToPaymentType = getPaymentTypeMap();
	
	// add this to frontend
	// const currencies = [ "MXN","ARS","CLP","ZAR","INR","VND","THB","AUD","GHS","GBP","IDR","PHP","TRY","AED","RWF","EUR","COP","USD","MYR","EGP","NGN","PEN","KES","XAF","BRL" ];
	
	static {
		try {
			buildMaps();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
//		queryHistory();
//		queryLimits().display();
//		coinLimits().display();
//		getAllTransactions();
//		query( "https://api.onramp.money/onramp/api/v2/common/public/fetchPaymentMethodType").display();
//		getPrices().display();
		S.out( getQuote( "INR", 10000) );

//
//		JsonObject prices = getPrices();
//		prices.getObject( "data").getObject( "onramp").forEach( (key,val) -> 
//			S.out( "%s: %s", tokenMap.getString( key.toString() ), val) );
	}

	private static String wl = "https://api-test.onramp.money/onramp/api/v2/whiteLabel";
	
	public static JsonObject transact(
			String fromCustomerId,
			double amount,
			String currency,
			String toWalletAddr,
			double recAmt
			) throws Exception {
		
		Util.require( isValidCurrency( currency), "invalid currency");
		
		var body = Util.toJson(
				"fromCurrency", currency,
				"toCurrency", "USDT",
				"chain", "MATIC20",
				"paymentMethodType", mapFiatToPaymentType.get( currency),
				"depositAddress", toWalletAddr,
				"customerId", fromCustomerId,
				"fromAmt", amount,
				"toAmount", recAmt,
				"rate", mapFiatToRate.get( currency)
				);
		
		S.out( "sending onramp transaction: " + body);

		var resp = whiteLab( "/onramp/createTransaction", body);
		S.out( "  response: " + resp);
		return resp;
	}
	
	public static void getTransaction( String customerId, String transactionId) throws Exception {
		var resp = whiteLab( "/onramp/transaction", Util.toJson(
				"customerId", customerId,
				"transactionId", transactionId
				));
		resp.display();
	}
	
	public static void getUserTransactions( String customerId) throws Exception {
		var resp = whiteLab( "/onramp/allUserTransaction", Util.toJson(
				"customerId", customerId,
				"page", 1,
				"pageSize", "500"
				) );
		resp.display();
	}
		
	// should be used by Monitor?
	public static void getAllTransactions() throws Exception {
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

	
	private static JsonObject whiteLab(String uri, JsonObject json) throws Exception {
		Util.require( uri.startsWith( "/"), "start with /");
		return query( wl + uri, json);
	}
	
	/** why do I need the chain and payment method to get a quote? 
	 *  are there different prices on different chains and methods? */
	public static double getQuote( String currency, double fromAmt) throws Exception {
		S.out( "querying onramp quote  currency=%s  fromAmt=%s", currency, fromAmt);
		
		var json = whiteLab( "/onramp/quote", Util.toJson( 
				"fromCurrency", currency,
				"toCurrency", "USDT",
				"fromAmount", "" + fromAmt,
				"paymentMethodType", mapFiatToPaymentType.getString( currency),
				"chain", "MATIC20"
				) );

		S.out( "  onramp quote: " + json);

		var data = json.getObjectNN( "data");
		double toAmt = data.getDouble( "toAmount");

		if (toAmt <= 0) {
			throw new MyException( "Error: could not get onramp quote  current=%s  fromAmt=%s",
					currency, fromAmt);
		}
		
		// save the rate; we'll use it later when creating the order
		mapFiatToRate.put( currency, data.getDouble( "rate"));
		
		return toAmt;
	}
	
	private static JsonObject getCustReq( String wallet, String phone) {
		return Util.toJson(
				"clientCustomerId", wallet.toLowerCase(),
				"phoneNumber", phone,
				"type", "INDIVIDUAL",  		// individual or business
				"kycRedirectUrl", "https://reflection.trading",  // user is redirected here after kyc
				"closeAfterLogin", true);
	}

	public static String getCustomerId( String wallet, String phone) throws Exception {
		var json = whiteLab( "/kyc/url", getCustReq( wallet, phone) );
		return json.has( "customerId") 
				? json.getString( "customerId")  // if it's a subsequent time
				: json.getObjectNN( "data").getString( "customerId");  // if it's the first time
	}
	
	/** first call; customer id will be assigned 
	 *  fields are url customerId and status*/
	public static JsonObject getKycUrl( String wallet, String phone) throws Exception {
		return getKycUrl( getCustReq( wallet, phone) );
	}

	/** subsequent call; wallet and phone can change
	 * fields are url customerId and status */
	public static JsonObject getKycUrl( String custId, String wallet, String phone) throws Exception {
		return getKycUrl( getCustReq( wallet, phone).append( "customerId", custId) );
	}

	/** all calls */
	public static JsonObject getKycUrl( JsonObject req) throws Exception {
		// try first w/out customerId
		var json = whiteLab( "/kyc/url", req);
		json.display();
		
		if (json.has( "error")) {
			throw new Exception( "Could not get KYC URL - " + json.getString( "error") ); 
		}
		
		var data = json.getObject( "data");
		Util.require( data != null, "Could not get KYC URL - no data was returned");
		
		var url = data.getString( "kycUrl");
		Util.require( S.isNotNull( url), "Error: no KYC URL returned");
		
		var custId = data.getString( "customerId");
		Util.require( S.isNotNull( custId), "Error: no customer ID returned");
			
		return Util.toJson( 
				"url", url, 
				"customerId", custId, 
				"status", getKycStatus( custId)
				);
	}

	/** 'data' could be json containing the status, or a string containing 'LOGIN_REQUIRED' */
	private static String getKycStatus( String customerId) throws Exception {
		var json = whiteLab( "/kyc/status", Util.toJson( "customerId", customerId) );
		String data = json.getString( "data");
		return JsonObject.isObject( data) ? json.getObject( "data").getString( "status") : data;
	}

	/** build maps of currency name to currency id */
	public static void buildMaps() throws Exception {
//		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/allConfigMapping";
		
//		var data = query( url).getObject( "data");
//		data.display();
		
//		mapFiatToId = data.getObject("fiatSymbolMapping");

//		mapFiatToId.getObject( "data").getObject( "coinSymbolMapping").forEach( (key,val) -> 
//			mapIdToFiat.put( val.toString(), key) );
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
	public static JsonObject coinLimits() throws Exception {
		String url = "https://api.onramp.money/onramp/api/v2/common/transaction/allCoinLimits";
		return query( url, Util.toJson( "fiatType", 1) );  // 1=onramp, 2=offramp, 3=both
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
	
	private static JsonObject query( String url) throws Exception {
		return query( url, new JsonObject() );
	}
	
	private static JsonObject query( String url, JsonObject body) throws Exception {
		JsonObject payload = Util.toJson( 
				"timestamp", System.currentTimeMillis() - 30000,
				"body", body);

		String encodedPayload = Encrypt.encode( payload.toString() ); 

		// create the signature
		SecretKeySpec keySpec   = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");  // Create HMAC SHA256 key from secret
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(keySpec);
		byte[] result = mac.doFinal( encodedPayload.getBytes() );
		String signature = Encrypt.bytesToHex(result);

		S.out( "url: " + url);
		S.out( "sending body: " + body);
//		S.out( "payload: " + payload);
//		S.out( "encoded payload: " + encodedPayload);
//		S.out( "signature: " + signature);

		String str = MyClient.create(url, body.toString() )
				.header("Accept", "application/json")
				.header("Content-Type", "application/json;charset=UTF-8")
				.header("X-ONRAMP-APIKEY", apiKey)
				.header("X-ONRAMP-PAYLOAD", encodedPayload)
				.header("X-ONRAMP-SIGNATURE", signature)
				.query().body();

		return JsonObject.parse( str);
	}
	
	static void listAllCoins() throws Exception {
		query( "https://api.onramp.money/onramp/api/v3/buy/public/listAllCoins").display();
		query( "https://api.onramp.money/onramp/api/v3/buy/public/listAllNetworks").display();
		query( "https://api.onramp.money/onramp/api/v2/common/public/fetchPaymentMethodType").display();
		MyClient.getJson( "https://api.onramp.money/onramp/api/v2/common/public/fetchPaymentMethodType").display();  // get payment method types
	}

	private static JsonObject getPaymentTypeMap() {
		return Util.toJson( 
			"INR", "IMPS",
			"TRY", "TRY_BANK_TRANSFER",
			"AED", "AED-BANK-TRANFER",
			"MXN", "SPEI",
			"EUR", "SEPA_BANK_TRANSFER",
			"IDR", "IDR_BANK_TRANSFER",
			"GBP", "FASTER_PAYMENTS",
			"VND", "VIET-QR",
			"NGN", "NG-BANK-TRANSFER",
			"BRL", "PIX",
			"ZAR", "ZAR-BANK-TRANSFER",
			"THB", "THAI_QR"
			);
	}


	public static boolean isValidCurrency(String currency) {
		return mapFiatToPaymentType.get( currency) != null;
	}

}

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

/* fiat -> id 
"MXN" : 4,
"ARS" : 29,
"CLP" : 10,
"ZAR" : 17,
"INR" : 1,
"VND" : 5,
"THB" : 27,
"AUD" : 30,
"GHS" : 16,
"GBP" : 20,
"IDR" : 14,
"PHP" : 11,
"TRY" : 2,
"AED" : 3,
"RWF" : 18,
"EUR" : 12,
"COP" : 9,
"USD" : 21,
"MYR" : 28,
"EGP" : 31,
"NGN" : 6,
"PEN" : 8,
"KES" : 15,
"XAF" : 19,
"BRL" : 7

payment types
For INR -> UPI
For TRY -> TRY_BANK_TRANSFER
For AED -> AED-BANK-TRANFER
For MXN -> SPEI
For EUR -> SEPA_BANK_TRANSFER
For IDR ->  IDR_BANK_TRANSFER
For GBP -> FASTER_PAYMENTS
For VND -> VIET-QR
For NGN -> NG-BANK-TRANSFER
For BRL -> PIX
For ZAR -> ZAR-BANK-TRANSFER
For THB -> THAI_QR
*/