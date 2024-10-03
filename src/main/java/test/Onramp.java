//package test;
//
//import org.json.simple.JsonObject;
//
//import common.Util;
//import http.MyClient;
//
//public class Onramp {
//	static String ramp = "https://api-test.onramp.money/onramp/api/v2/whiteLabel";
//	static String otp = "123456";
//
//
//	void getKycUrl() throws Exception {
//		JsonObject params = Util.toJson(
//				"customerId", "abc",
//				"email", "abc",
//				"phoneNumber*", "123",
//				"clientCustomerId*", "abc",
//				"type", "type",
//				"kycRedirectUrl", "https://reflection.trading",
//				"closeAfterLogin", true
//				);
//
//		post( "/kyc/url", params);
//	}
//
//
//	private JsonObject post(String uri, JsonObject json) throws Exception {
//		String body = json.toString();
//		String payload = Encrypt.encode( body);
//		signature = 
//		async function generatePayloadAndSignature(secret, body) {
//			  const timestamp = Date.now().toString();
//			  const obj = {
//			    body,
//			    timestamp
//			  };
//			  const payload = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(JSON.stringify(obj)));
//			  const signature = CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA512(payload, secret));
//			  return { payload, signature };
//			}
//		
//		
//		
//		return MyClient.create( ramp + uri, params.toString() )
//				.header("apikey", apiKey)
//				.header("payload", payload)
//				.header("signature", signature)
//			.queryToJson();
//		
//	}
//	
//	public static void main(String[] args) throws Exception {
//		new Onramp()
//			.getKycUrl();
//	}
//}
//
//
