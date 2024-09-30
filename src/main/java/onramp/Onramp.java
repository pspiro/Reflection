package onramp;

import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.checkerframework.common.reflection.qual.GetMethod;
import org.json.simple.JsonArray;
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

	static JsonObject fiatMap;  // map fiat id to fiat name
	static HashMap<String,Double> fiatRateMap = new HashMap<>();  // map fiat name to rate
	static JsonObject paymentMethodMap = new JsonObject();  // map fiat name to transfer method
	
	static String prod = "https://api.onramp.money/onramp/api/v2/whiteLabel";
	static String dev = "https://api-test.onramp.money/onramp/api/v2/whiteLabel";
	static String wlUrl = dev; 
	static boolean debug = true;
	
	public static void useProd() {
		wlUrl = prod;
	}
	
	// more:
	// url to add bank accounts https://api.onramp.money/onramp/api/v2/whiteLabel/bank/addFiatAccountUrl

	// other queries, no auth needed
	// https://api.onramp.money/onramp/api/v3/buy/public/listAllCoins
	// https://api.onramp.money/onramp/api/v3/buy/public/listAllNetworks
	// https://api.onramp.money/onramp/api/v2/common/public/fetchPaymentMethodType
	// https://docs.onramp.money/onramp-whitelabel-unlisted/whitelabel-public-endpoints/list-supported-fiat

	public static void setWhiteLabel( String url) {
		S.out( "Setting onramp white label url to " + url);
		wlUrl = url;
	}

	// add this to frontend
	// const currencies = [ "MXN","ARS","CLP","ZAR","INR","VND","THB","AUD","GHS","GBP","IDR","PHP","TRY","AED","RWF","EUR","COP","USD","MYR","EGP","NGN","PEN","KES","XAF","BRL" ];
	
	static {
		try {
			buildMaps();
			S.out( "fiat map: " + fiatMap);
			S.out( "payment method map: " + paymentMethodMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** these are the known statuses but there could be others */
	public enum KycStatus {
		LOGIN_REQUIRED,
		TEMPORARY_FAILED, 
		PERMANENT_FAILED, 
		BASIC_KYC_COMPLETED, 
		INTERMEDIATE_KYC_COMPLETED,
		ADVANCE_KYC_COMPLETED,
		IN_REVIEW, 
		COMPLETED;
	}
	

	public static void main(String[] args) throws Exception {
		fiatMap.display();
		paymentMethodMap.display();
		getQuote( "AED", 100);
	}
	
	public static JsonObject transact(
			String fromCustomerId,
			double amount,
			String currency,
			String toWalletAddr,
			double recAmt
			) throws Exception {
		
		Util.require( isValidCurrency( currency), "Invalid currency");
		
		String paymentType = paymentMethodMap.getString( currency);
		Util.require( S.isNotNull( paymentType), "No payment type available for " + currency);
		
		var body = Util.toJson(
				"fromCurrency", currency,
				"toCurrency", "USDT",
				"chain", "MATIC20",
				"paymentMethodType", paymentType,
				"depositAddress", toWalletAddr,
				"customerId", fromCustomerId,
				"fromAmount", amount,
				"toAmount", recAmt,
				"rate", fiatRateMap.get( currency)
				);
		
		return whiteLab( "/onramp/createTransaction", body);
	}
	
	public static JsonObject updateKycStatus(String customerId, KycStatus status) throws Exception {
		return whiteLab( "/test/changeKycStatus", Util.toJson(
				"customerId", customerId,
				"status", status
				));
	}

	// Transaction status
//	-2, -1 : FAILED
//	2, 10: FIAT_DEPOSIT_RECEIVED
//	3, 13: TRADE_COMPLETED
//	14: ON_CHAIN_INITIATED
//	4, 15: ON_CHAIN_COMPLETED
	
	public static int getTransStatus( String customerId, String transactionId) throws Exception {
		var resp = whiteLab( "/onramp/transaction", Util.toJson(
				"customerId", customerId,
				"transactionId", transactionId
				) );
		return resp.getObjectNN("data").getInt( "status");
	}
	
	public static JsonObject setTransStatus( String customerId, String transactionId, int status) throws Exception {
		return whiteLab( "test/changeOnrampStatus", Util.toJson(
				"customerId", customerId,
				"transactionId", transactionId,
				"status", "" + status
				) ); 
	}
	
	public static JsonArray getUserTransactions( String customerId) throws Exception {
		return whiteLab( "/onramp/allUserTransaction", Util.toJson(
				"customerId", customerId,
				"page", 1,
				"pageSize", "500"
				) ).getArray("data");
	}
		
	// should be used by Monitor?
	public static JsonArray getAllTransactions() throws Exception {
		return whiteLab( "/onramp/allTransaction", Util.toJson(
				"page", 1,
				"pageSize", "500") ).getArray( "data");
	}
		
	/** why do I need the chain and payment method to get a quote? 
	 *  are there different prices on different chains and methods? */
	public static double getQuote( String currency, double fromAmt) throws Exception {
		var json = whiteLab( "/onramp/quote", Util.toJson( 
				"fromCurrency", currency,
				"toCurrency", "USDT",
				"fromAmount", "" + fromAmt,
				"paymentMethodType", paymentMethodMap.getString( currency),
				"chain", "MATIC20"
				) );
		
		var data = json.getObjectNN( "data");
		double toAmt = data.getDouble( "toAmount");

		if (toAmt <= 0) {
			throw new MyException( "Error: could not get onramp quote  currency=%s  fromAmt=%s",
					currency, fromAmt);
		}
		
		// save the rate; we'll use it later when creating the order
		fiatRateMap.put( currency, data.getDouble( "rate"));
		
		return toAmt;
	}
	
	/** first call; customer id will be assigned 
	 *  fields are url customerId
	 *  WARNING: if you get 'Please provide a valid phone number string, it may
	 *  mean that your wallet is already associated with a different phone number */ 
	public static JsonObject getKycUrlFirst( String wallet, String phone, String redirectUrl) throws Exception {
		var req = Util.toJson(
				"clientCustomerId", wallet.toLowerCase(),
				"phoneNumber", phone,
				"type", "INDIVIDUAL",  		// individual or business
				"kycRedirectUrl", redirectUrl);  // user is redirected here after kyc
		return getKycUrl( req);
	}

	/** subsequent call; wallet and phone can change
	 * fields are url customerId */
	public static JsonObject getKycUrlNext( String custId, String redirectUrl) throws Exception {
		var req = Util.toJson(
				"customerId", custId,
				"type", "INDIVIDUAL",  			  // individual or business
				"kycRedirectUrl", redirectUrl);  // user is redirected here after kyc
		return getKycUrl( req);
	}

	/** all calls */
	public static JsonObject getKycUrl( JsonObject req) throws Exception {
		var json = whiteLab( "/kyc/url", req);
		
		if (json.has( "error")) {
			throw new Exception( "Could not get KYC URL - " + json.getString( "error") ); 
		}
		
		var data = json.getObject( "data");
		Util.require( data != null, "Could not get KYC URL - no data was returned");
		
		var url = data.getString( "kycUrl");
		Util.require( S.isNotNull( url), "Error: no KYC URL returned");
		
		var custId = data.getString( "customerId");
		Util.require( S.isNotNull( custId), "Error: no customer ID returned");
		Util.require( !req.has( "customerId") || req.getString( "customerId").equals( custId), "Error: mismatched customer ID");  // cust id should not change
			
		return Util.toJson( 
				"url", url, 
				"customerId", custId
				);
	}

	/** 'data' could be json containing the status, or a string containing 'LOGIN_REQUIRED' */
	public static String getKycStatus( String customerId) throws Exception {
		var json = whiteLab( "/kyc/status", Util.toJson( "customerId", customerId) );
		String data = json.getString( "data");
		return JsonObject.isObject( data) ? json.getObject( "data").getString( "status") : data;
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
		MyClient.getJson( url).display();
	}
	public static void queryHistory() throws Exception {

		String url = "https://api.onramp.money/onramp/api/v1/transaction/merchantHistory";

		JsonObject query = Util.toJson(
				"page", 1,
				"pageSize", 50   // Min: 1, Max: 500, Default: 50
				//				"since", "2022-10-07T22:29:52.000Z"
				);
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

		return query( url, query);
	}
	
	private static JsonObject whiteLab(String uri, JsonObject json) throws Exception {
		Util.require( uri.startsWith( "/"), "start with /");
		return query( wlUrl + uri, json);
	}
	
	private static JsonObject query( String url) throws Exception {
		return query( url, new JsonObject() );
	}

	/** @return an ORDERED json object, so we don't lose the order of the Bank
	 *  section when we send the createTransaction endpoint */
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

		out( "Sending onramp request");
		out( "  request url: " + url);
		out( "  request body: " + body);
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
		
		out( "  onramp response: " + str);

		return JsonObject.parseOrdered( str);  // NOTE this calls parseORDERED
	}
	
	static void out( String str) {
		if (debug) S.out( str);
	}
		
	/** Returns a map of currency id to currency, e.g. '1': 'INR'
	 *  Not currently used. No credentials required 
	 * @throws Exception */
	static JsonObject getCurrencies() throws Exception {
		return MyClient
				.getJson( "https://api.onramp.money/onramp/api/v2/whiteLabel/public/listSupportedFiat")
				.getObject( "data")
				.getObject( "onramp");
	}
	

	public static void buildMaps() throws Exception {
		try {
			// map currency id to currency name
			fiatMap = MyClient.getJson( "https://api.onramp.money/onramp/api/v2/whiteLabel/public/listSupportedFiat")
					.getObjectNN( "data").getObject( "onramp");
			Util.require( fiatMap != null, "Error: no fiat map returned");
		}
		catch(Exception e) {
			e.printStackTrace();
			S.out( "using default map");
			fiatMap = getDefaultFiatMap();
		}
		
		// build payment methods map (map fiat name to method)
		try {
			JsonObject pmtMethods = MyClient.getJson( "https://api.onramp.money/onramp/api/v2/common/public/fetchPaymentMethodType")
				.getObject( "data");
			Util.require( pmtMethods != null, "null payment methods");

			Util.forEach( pmtMethods, (fiatId, obj) -> {
				if (obj instanceof JsonObject submap) {
					Util.forEach( submap, (method, methodType) -> {
						Util.iff( fiatMap.getString( fiatId), name -> 
							paymentMethodMap.put( name, method) );
					});
				}
				else {
					S.out( "Error: payment method map has unexpected format");
				}
			});
		}
		catch( Exception e) {
			e.printStackTrace();
			paymentMethodMap = getDefaultPaymentMethodMap();
		}
	}
	
	/** @param fiat ID
	 * @return fiat name, e.g. EUR */
//	private static String fiatName(String fiatId) throws Exception {
//		Util.require( fiatMap.has( fiatId), "Invalid fiatId " + fiatId);
//		return fiatMap.getString( fiatId);
//	}
	
	public static boolean isValidCurrency(String currency) {
		return paymentMethodMap.get( currency) != null;
	}
	
	private static JsonObject getDefaultPaymentMethodMap() {
		return Util.toJson(
			"AED", "AED-BANK-TRANSFER", 
			"ARS", "WIREAR", 
			"BRL", "PIX",
			"CLP", "KHIPU", 
			"EUR", "SEPA_BANK_TRANSFER", 
			"GBP", "FASTER_PAYMENTS", 
			"IDR", "IDR_BANK_TRANSFER", 
			"INR", "UPI", 
			"MXN", "SPEI", 
			"NGN", "NG-BANK-TRANSFER", 
			"PEN", "KHIPU", 
			"TRY", "TRY_BANK_TRANSFER", 
			"VND", "VIET-QR" 
			);
	}
	
	static JsonObject getDefaultFiatMap() {
		return Util.toJson(
			"1", "INR",
			"2", "TRY",
			"3", "AED",
			"4", "MXN",
			"5", "VND",
			"6", "NGN",
			"7", "BRL",
			"8", "PEN",
			"10", "CLP",
			"11", "PHP",
			"12", "EUR",
			"14", "IDR",
			"20", "GBP",
			"29", "ARS"
			);
	}

	public static void debugOff() {
		debug = false;
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
