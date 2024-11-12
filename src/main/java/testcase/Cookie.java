package testcase;

import org.json.simple.JsonObject;
import org.web3j.crypto.Keys;

import com.moonstoneid.siwe.SiweMessage;

import common.Util;
import http.MyClient;
import http.MyHttpClient;
import reflection.SiweUtil;
import tw.util.S;

/** NOTE: if you use Cookie, you must be using config.txt; you cannot use Config.ask()
 *  because Cookie and testcase infra. will re-read Config from config.txt */
public abstract class Cookie extends MyTestCase {
	//public static String wallet = "0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3";
	//public static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	//public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	//public static String wallet = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
	//public static String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
	public static String wallet   = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
	public static String cookie;  // that's right, the cookie is a string, not an object
	public static boolean init; // to force initialization
	public static String prodWallet = "0x2703161D6DD37301CEd98ff717795E14427a462B";
	
	static {
		try {
			cookie = signIn(wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void setNewFakeAddress(boolean andProfile, boolean andKyc) throws Exception {
		setNewFakeAddress( andProfile);
		
		if (andKyc) {
			// insert/update (record may or may not exist)
			var json = Util.toJson( 
					"wallet_public_key", wallet.toLowerCase(),
					"kyc_status", "VERIFIED");
			
			m_config.sqlCommand( sql -> 
				sql.insertOrUpdate("users", json, "where wallet_public_key = '%s'", wallet.toLowerCase() ) ); 
		}
	}

	public static void setNewFakeAddress(boolean andProfile) throws Exception {
		setWalletAddr( Util.createFakeAddress() );
		
		if (andProfile) {
			JsonObject json = TestProfile.createValidProfile();
			json.put( "email", "test@test.com"); // recognized by RefAPI, non-production only
			MyClient.postToJson( "http://localhost:" + port + "/api/update-profile", json.toString() );
		}
	}
	
	public static void setWalletAddr(String walletIn) throws Exception {
		wallet = Keys.toChecksumAddress( walletIn);
		cookie = signIn(wallet);
	}

	public static String signIn(String address) throws Exception {
		S.out( "Signing in with cookie for wallet " + address);
		
		var client = new MyHttpClient("localhost", port);
		
		// send siwe/init
		client.get("/siwe/init");
		//assertEquals( 200, cli.getResponseCode() );
		String nonce = client.readJsonObject().getString("nonce");

		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				address, 
				"http://localhost", 
				"1",	// version 
				11155111,      // chainId, Sepolia 
				nonce,
				Util.isoNow() )
				.statement("Sign in to Reflection.")
				.build();
		
		JsonObject signedMsgSent = new JsonObject();
		signedMsgSent.put( "signature", "102268");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		client = new MyHttpClient("localhost", port);
		client.post("/siwe/signin", signedMsgSent.toString() );
		//assertEquals( 200, cli.getResponseCode() );
		
		return client.getHeaders().get("set-cookie");
		//S.out( "received cookie: " + cookie);
	}
	
	/** does not check for null */
	public static JsonObject getUser() throws Exception {
		return m_config.sqlQuery( "select * from users where wallet_public_key = '%s'", wallet.toLowerCase() ).get( 0);
	}

	public static JsonObject getJson() {
		return Util.toJson( 
				"cookie", cookie,
				"chainId", 1115511);
	}
}
