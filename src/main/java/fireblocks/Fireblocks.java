package fireblocks;

import static common.Util.rnd;

import java.math.BigInteger;
import java.net.http.HttpResponse;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import positions.Wallet;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;
import web3.RetVal;

/** This shit works. You pass in everthing ahead of time, then call transact() */
public class Fireblocks {
	static String testPk = "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";
	static String prodPk = "MIIJQQIBADANBgkqhkiG9w0BAQEFAASCCSswggknAgEAAoICAQDE32SMsTsel5eFdkUyfYcY+bA3WQM5HvHBeH0oh9NyLT1nve0Td4OAxCHLOKIYfb3so14Ytj+BOeTqR7p/kQU2hro6OLFKO1Ye8+LER53l5NKTUkpvQ13Pd7k/82rOjBuWYFuHkyxwgbILaK0evBipJVhoH2XtNuTTNkSZgnu5xdEa5uBBHIE7CxRaMvVumRGKFdNlBtjmwxdEcnKMV+Fu8FS5OB7/ZxdCTDKw0tdte2vBtA3QcOdDAVe6yrVjKV4JQBKIfSzMh8fAN5JONxIn2GMjgwjeqetkcAuapubbu2oFbO6ZFcIWCUAJ8kpcKA78QpYZ5spJJEHv2n0IfT80kSR/hCRTRSxCKRSAdggm4mpcHRYq73g/HHrSyEIpRzltceGGmDmWLXeNxsZBGHszC8ogYNw34OaSwgGksG7Fu7SW4Ihbb5rgZHdkgxgTWeRt/nwnsyU3X9Mr1mvQbnc1DaYn/cw12Mc3Q57EDwX35nKbGEZNaBThrN31lp/ILD5olyS1czVOrjeVmvd56T8VZwIkcngFF4Er/2smFGaTXdvdSkcreaj9dOhZnZQtSNb9OA767w925895SdG5jl1alTGQLdwZg1eJ8plrtjYjTQdV/QjMhVrEU0/Gzvqlz+nb/s1zvWov7wim+NvGhXgAej2UFvv+jCae7rIa5bq5aQIDAQABAoICABMGN1Q1KC5bncPC2wr3AGAUeMfQbTRO3jPA7e9FuNQWtRFaVBeGfAmsakuWDGvrcrf0TXp72gUl0A4nokkDfYp4WHSZ1+ABfd/911L6TLLTetnLoO/BhcYvnpIkdpTBC0VryEyAl1HD5q2djuUibguiVTmylwnmPRR/8P8YkdYw3HUKDG+eToqjFnNk6zylxAEg/M2NbNZX0d7GZHVVZK5wwO8pEi3CC4hXjqpxpWmp/I66VH9o+aAK+CxPvFl5vOGN9HyGBBc7o7zaYeP3BGUfTu02k1vY9as9/JEUQcjIAoXFsgTPCnNQr4SGilL/d/s+f/y+xZttyoVgCqcQW1QQshXmzaQb4hvyxeS1EeMUAMYPJj89oSU9UNBXIat8XXlbGkNPwOBINuJUbeWIla0SEmXT12tyuEyXRqWfExhYLrQ9EsO33ClLc30nYDCxhU6UeuMm8vaTQ+h5/JY5zowv14/CgE7hS2FGdAAFn2XT8krKsv1wA1407/v/5eoYyt+UEek1xyrwB9bsJPTfX9wHhMX97Dn1q6hQjdiTrPxL4aVdhHZbbZZk3K4/UzX8ukP4VKksijwif3cv4++Y7b99qJT7ag4lSBsAAT/fUi1Zoxc05d1rITuP6AsP4C1B8vyoHgjTDzsFFaGIHY/0W0QTSN7/18TVG6PeABnqIc0RAoIBAQDjlL3wB2W14ACRf5yhN3WLUfn9GGnJbMyoEX9k+VSI37rgs6JW7nHRt/TQw5EzhG/NujFhp9nql9tUtxH+9a6Hsu73xPimnW7GeSe7fz5uQImuZWTUiHe9g3VKE1x7MvPU11bckQ6IIx2Vg7imVbiOnbY1l5adZX/BLpAkJfhTmOOlmgVC5eqZ6zNHiQWCJFGFe3vzy0z8IV9ibxkiMCDxkI3/ZoC9IZkHYBR88r826mCV1OPA+O9/EP2UzdxU3ctHbbmPpdyZ1vIZRAJUtaxCmGjx1XNKSNujCTTV8dvRP9H/cqVHVaoQfqfHFskHcNNthtJC7gPbAn4xOv99U4z1AoIBAQDddPjNFna8Hk1HeCEx93PcarkJtKdSvLVBb4Y+DejVHDmjWD0GEOQKnMVp713GhExT3y/ygG6HrfDiq6x0PkxjistzQL8geHxYT/LZ5REppjt5TTnhzRLQ0aTEKtjUoo/zVKI8g5OVl6rYs1cD+jQhx+3HZ9i/QNDzZfMs85EUt3PEFhix+O/ve1uTbR67jzVAsK16qfmjFx52K/y3IbYEFeaXAF0g3IfcXqE2E6c7GvL0GUl1Vmkkm1PqkiJQM7/V6EYvpyhdD+HHS3j2DhSogrL/HHTMsDfz62rOVnodDXShLmI+ngoUrgeIJmhA0YRXawh1jfjgvZNGBBtG8bIlAoIBAHpj6sXkZImsPwIAnA6Dkd5sJsI7V+DwdxvH1ThLcm/tykW/tYWE1IdRpAKFulf3WF1OTfJT37jvFBB6J95y5/qoeyWas5J/RizMyVZndv66DJFSjChUf/jWcsR0px7GZyVG7brO8/64y9c1sJ8bqJZiMLSfomthQ5Rz4ybdmK/0oDDWJRA2L7W5LKnDAchcmc7setvAsNVDaVW5o82kDoN7FzKMQJ32LKMvw7Tyo2z4HRtXE2kbN0mhrI5TK7QkV4dBdjLPo1TEqQx3a718unhSGNY90bPYfO4wLE8GtA3Tga8cS9CSPFSEmNxT36b1wW68u+UstbwZmh4FuZL/1OUCggEAOr4UYRJcXHoNNa//3iktaoJEovZwi9nSmpDiowM9h4n9H/7/8OP6GXaMRxVfYpElTfod1c2LhCH3i6jODeeYi679WI0MJajQEvziBjpk2w9lJz/84pBrNUjm08Ip0f1tAbapcjPUqhGZF/I6Wqu7uFo4EHTTYafnxSh94KabDUu8QX86/bpyqqumFu0TAd1y2r4Cxk7gEQrI4b+5QTqH9X15tQQ7r8PPpfzT9mwwJ9V2LJmoip9pNHSfRdGNmIihkjQSqgFFC6py0VKLu3jwseI5aMI7mTAFJ4aCxkjaC2rOTRLorINvWRzwkoTdkpQA7s9NZyFJqxOeRSxBsAfcoQKCAQBUYwBTW8RLEa0oToU3xBMG27jrFGq6OKC/z+REiXghjpZC5kKZLEEGD8TYC7AC9fazBVdM0NtjLkLpU33rqNlTX+86yO2t8bY+Nk9tM7+eFbebRG1ejcroKksMysBlvhAjOfeP2QNfawekdZOuF9VB96Qh7aOjxdJHPMuNIsltRxeBNnCmz9jPE+b6SXpCgNMQGL41SHRzcBbFWH3gPhdpIcF+/DQQ1d1oZaRZblbZZHIFaG0kuZtgoDq8xLLWEtdFA+FzoEkVdX1Ce7JeGNqtzyzghTprjQ7PdrbTCcbtKq6IlZuxWmLfYaFKF6RHhyKJC3E/m7GdUHKYiCkV0Af1";
	static String testApi = "bbce654d-08da-2216-5b20-bed4deaad1be";
	static String prodApi = "e3f1c5b4-48f3-f495-7b14-23e729bc3628";

	public static String testBusd = "BUSD_ETH_TEST3_6ZNB";  // Fireblocks asset id
	public static String testRusd = "RUSD_ETH_TEST3_S89L";
	
	static String baseUrl = "https://api.fireblocks.io";
	
	private static String s_apiKey;
	private static String s_privateKey;
	public static String platformBase;   // base currency of the platform, e.g. ETHER or MATIC
	
	private String endpoint;
	private String body = "";  // optional
	
	public void body(String v) { body = v;	}
	public void endpoint(String v) { endpoint = v;	}
	
	public static void setKeys( String apiKey, String privateKey, String _platformBase) {
		s_apiKey = apiKey;
		s_privateKey = privateKey;
		platformBase = _platformBase;
		
		S.out( "Platform native token is %s", platformBase);
	}
	
	/** Returns the Fireblocks ID. Throws exception if there is no id */
	String transactToId() throws Exception {
		JsonObject obj = transactToObj();
		String id = obj.getString("id");
		Util.require( S.isNotNull(id), "Fireblocks error: " + obj.getString("message") );
		return id;
	}
		
	/** Returns MyJsonObject */
	JsonObject transactToObj() throws Exception {
		return JsonObject.parse( transact() );
	}
			
	String transact() throws Exception {
		long start = System.currentTimeMillis() / 1000 - 5;  // minus 5 because you get an error if your timestamp is in the future
		long expire = start + 30;

		// toJson removes spaces in the values, not good
		String bodyHash = Encrypt.getSHA256(body);

		String header = toJson( "{ 'alg': 'RS256', 'typ': 'JWT' }");

		String nonce = String.valueOf( rnd.nextInt() );
		
		String payload = toJson( String.format( "{ "
				+ "'uri': '%s',"
				+ "'nonce': '%s',"
				+ "'iat': %s,"
				+ "'exp': %s,"
				+ "'sub': '%s',"
				+ "'bodyHash': '%s'"
				+ "}",
				endpoint, nonce, start, expire, s_apiKey, bodyHash) );
		
		String input = String.format( "%s.%s",
				Encrypt.encode( header),
				Encrypt.encode( payload) );
		
		Util.require( s_privateKey != null, "You must set key vals first");
		String signed = Encrypt.signRSA( input, s_privateKey);

		String jwt = String.format( "%s.%s.%s",
				Encrypt.encode( header), Encrypt.encode( payload), signed)
				.replace( "/", "_").replace( "+", "-");
		
		String url = baseUrl + endpoint;
		MyClient client = S.isNotNull(body) 
				? MyClient.create( url, body)
				: MyClient.create( url);
		HttpResponse<String> response = client
			.header("X-API-Key", s_apiKey)
			//.header("Connection", "close") works with AsyncHttpClient but not HttpClient
			.header("Content-type", "application/json")
			.header("Accept", "application/json, text/plain, */*")
			.header("Authorization", "Bearer " + jwt)
			.query();
		
		Util.require( 
				response.statusCode() == 200, 
				"Error status code %s - %s", 
				response.statusCode(), response.body() );
		
		return response.body();
	}
	
	/** Call a Fireblocks GET endpoint, return json object */
	public static JsonObject fetchObject(String endpoint) throws Exception {
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		return fb.transactToObj();
	}

	/** Call a Fireblocks GET endpoint, return the string (could be array) */
	public static JsonArray fetchArray(String endpoint) throws Exception {
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		String ret = fb.transact();
		if (!JsonArray.isArray(ret) ) {
			throw new Exception( JsonObject.parse(ret).getString("message") );  
		}
		return JsonArray.parse(ret);
	}

	public static String padInt(int amt) {
		return padLeft( String.format( "%x", amt) );  // hex encoding
	}
	
	public static String padBigInt(BigInteger amt) {
		return padLeft( amt.toString(16) );  // hex encoding
	}
	
	/** Assume address starts with 0x */
	public static String padAddr(String addr) throws Exception {
		Util.reqValidAddress(addr);
		return padLeft( addr.substring(2) );
	}

	/** Pad with left zeros to 64 characters which is 32 bytes */
	private static String padLeft(String str) {
		return Util.padLeft( str, 64, '0'); 
	}
	
	private static String padRight(String str) {
		return Util.padRight( str, 64, '0'); 
	}
	
	public static void main(String[] args) throws Exception {
		S.out( encodeParameters( 
				new String[] { "string", "address", "string"}, 
				new Object[] { "0x79da550edc0874fd3da72a70577081fd59fa7ddb", "0x79da550edc0874fd3da72a70577081fd59fa7ddb", "ABC"  }
				) );
	}
	
	/** For encoding parameters for deployment or contract calls.
	 *  Assume all string length <= 32 bytes
	 *  Encoded as follows: 64 characters per line, one line per parameter
	 *  add the strings at the end: one line for the length, then the data  
	 * @throws RefException */
	public static String encodeParameters( String[] types, Object[] params) throws Exception {
		Util.require( 
				types == null && params == null || 
				types != null && params != null && types.length == params.length, 
				"types and params are out of sync");
		
		// no parameters passed?
		if (types == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		int offsetBytes = types.length * 32;
		
		// encode the parameters; strings just get a placeholder
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			
			if (type.equals("string") ) {
				Util.require( val instanceof String, "Wrong type");
				
				String str = (String)val;
				Util.require( str.length() <= 100, "That's an awfully long string");

				// encode the starting offset for the string
				sb.append( padInt( offsetBytes) );

				offsetBytes += 32 + stringBytes( str.length() );  // add 32 for the row which encodes the string length
			}
			else if (type.equals( "address") ) {
				sb.append( padAddr( (String)val) );
			}
			else if (type.equals( "uint256") ) {
				Util.require( val instanceof Integer || val instanceof BigInteger, "Bad parameter type " + val.getClass() );
				if (val instanceof Integer) {
					sb.append( padInt( (Integer)val) );
				}
				else {
					sb.append( padBigInt( (BigInteger)val ) );
				}
			}
			else {
				Util.require( false, "Unexpected Fireblocks parameter type " + type);
			}
		}
		
		// encode the strings
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			if (type.equals("string") ) {
				sb.append( padInt( ((String)val).length() ) );  // encode the length (one row)
				encodeString(sb, (String)val);   // encode the string (could be one or more rows)
			}
		}
		return sb.toString();
	}

	/** Encode the string one row of 32 bytes at a time */
	private static void encodeString(StringBuilder sb, String val) {
		while (val.length() > 0) {
			sb.append( padRight( stringToBytes( Util.left(val, 32) ) ) );
			val = Util.substring(val, 32);
		}
	}

	/** Return the number of rows it takes to encode the string * 32
	 *  as there are 32 bytes per row */
	private static int stringBytes(int length) {
		return (int)Math.ceil(length / 32.0) * 32;
	}
	
	/** Return each byte's hex value */
	public static String stringToBytes(String val) {
		StringBuilder sb = new StringBuilder();
		for (byte b : val.getBytes() ) {
			sb.append( String.format( "%2x", b) );
		}
		return sb.toString();
	}
	
	/** @param addr is the address of the contract for which you are calling a method OR 0x0 when deploying a contract 
	 *  @param callData is keccak for call or bytecode for deploy; can start w/ 0x or not
	 *  @param params are appended to the call data
	 *  @return RetVal so caller can either get ID or wait for blockchain hash */
	public static FbRetVal call2(int fromAcct, String addr, String callData, String[] paramTypes, Object[] params, String note) throws Exception {
		note = note.replaceAll( " ", "-");  // Fireblocks doesn't like spaces in the note
		
		String bodyTemplate = 
				"{" + 
				"'operation': 'CONTRACT_CALL'," + 
				"'amount': '0'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"}," + 
				"'extraParameters': {" +
				"   'contractCallData': '%s'" +   //0x seems optional
				"}," +
				"'note': '%s'" + 
				"}";

		String fullCallData = callData + encodeParameters( paramTypes, params);  // call data + parameters 
		
		String body = toJson( 
				String.format( bodyTemplate, Fireblocks.platformBase, fromAcct, addr, fullCallData, note) );
		
		S.out( body);

		Fireblocks fb = new Fireblocks();
		fb.endpoint( "/v1/transactions");
		fb.body( body);
		
		return fb.transactToRetVal();
	}

	/** @param amount is the real world dollar amt
	 * @param token is the name of the token on Fireblocks */
	public static RetVal transfer(int fromAcct, String dest, String token, double amount, String note) throws Exception {
		Util.reqValidAddress(dest);
		Util.require( S.isNotNull(token), "Null token name");
		Util.require( S.isNotNull(note), "You must supply a note");
		
		note = note.replaceAll( " ", "-");  // Fireblocks doesn't like spaces in the note
		
		String bodyTemplate = 
				"{" + 
				"'operation': 'TRANSFER'," + 
				"'amount': '%s'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"}," + 
				"'note': '%s'" + 
				"}";

		String body = toJson( 
				String.format( bodyTemplate, amount, token, fromAcct, dest, note) );
		
		S.out( body);

		Fireblocks fb = new Fireblocks();
		fb.endpoint( "/v1/transactions");
		fb.body( body);
		return fb.transactToRetVal();
	}
	
	private FbRetVal transactToRetVal() throws Exception {
		JsonObject obj = transactToObj();
		String str = obj.getString("message");
		Main.require( S.isNull( str), RefCode.BLOCKCHAIN_FAILED, "Error on Fireblocks.transact  msg=%s  code=%s",
				str, obj.getString("code") );
		return new FbRetVal( obj.getString("id") );
	}
	
	public static String toJson( String format, Object... params) {
		return String.format( format, params).replaceAll( "\\'", "\"").replaceAll( " ", "");
	}
	
	/** Query the transaction from Fireblocks until it contains the txHash value
	 *  which is the blockchain transaction has; takes about 13 seconds
	 *  We get the hash in the CONFIRMING status; it eventually goes to COMPLETED status
	 *  
	 *  PERFORMANCE NOTE - we get a response 3-5 seconds sooner here than 
	 *  in the Fireblocks webhook callback, at least for the CONFIRMING status message
	 *  
	 *  The Moralis is WAY MORE delayed, even	. */
	public static String waitForHash(String fireblocksId, int tries, int sleep) throws Exception {
		// it always takes at least a few seconds, I think
		
		for (int i = 0; i < tries; i++) {
			S.sleep(sleep);
			JsonObject trans = Transactions.getTransaction( fireblocksId);
			S.out( "%s  %s  hash: %s", fireblocksId, trans.getString("status"), trans.getString("txHash") );
			
			String txHash = trans.getString("txHash");
			if (S.isNotNull( txHash) ) {
				return txHash;
			}
			
			String status = trans.getString("status");
			if ("COMPLETED".equals(status) ) {
				throw new RefException( RefCode.BLOCKCHAIN_FAILED, "Transaction completed with no transaction hash");  // should never happen
			}
			
			if ("FAILED".equals(status) ) {
				throw new RefException( RefCode.BLOCKCHAIN_FAILED, "Transaction failed - %s", trans.getString("subStatus") );
			}
		}
		
		throw new RefException( RefCode.TIMED_OUT, "Timed out waiting for transaction hash"); // should never happen
	}
	
	/** Wait for up to two minutes */
	public static void waitForStatus(String fireblocksId, String statusIn) throws Exception {
		// it always takes at least a few seconds, I think

		for (int i = 0; i < 120; i++) {
			S.sleep(1000);
			
			// if there is an error, keep trying, it might go away, e.g. 402
			JsonObject trans;
			try {
				trans = Transactions.getTransaction( fireblocksId);
			}
			catch( Exception e) {
				S.err( "Error while checking status", e);
				continue;
			}
			
			S.out( "%s  %s  hash: %s", fireblocksId, trans.getString("status"), trans.getString("txHash") );
			
			String status = trans.getString("status");
			Util.require( status != null, "Null Fireblocks status");
			
			if (status.equals( statusIn) ) {
				return;
			}
			
			if ("COMPLETED".equals(status) ) {
				throw new RefException( RefCode.BLOCKCHAIN_FAILED, "Transaction completed without status");  // should never happen
			}
			
			if ("FAILED".equals(status) ) {
				throw new RefException( RefCode.BLOCKCHAIN_FAILED, "Transaction failed - %s", trans.getString("subStatus") );
			}
		}
		throw new RefException( RefCode.TIMED_OUT, "Timed out waiting for Fireblocks transaction"); // should never happen
	}
	
	static void initMap() {
	}
	
	public static Wallet getWallet(String account) throws Exception {
		return Accounts.instance.getWallet(account);
	}

	/** Return true for goerli network */
	public static boolean isDev() {
		return platformBase == null || platformBase.startsWith( "ETH_TEST");
	}

	/** Use this to create wallet for native token when using a new testnet 
	 * @throws Exception */
	public static RetVal createWallet(int accountId, String assetId) throws Exception {
		Fireblocks fb = new Fireblocks();
		fb.endpoint( String.format( "/v1/vault/accounts/%s/%s", accountId, assetId) );
		return fb.transactToRetVal();
	}

	public static void transfer(String from, String to, double amt) {
		// TODO Auto-generated method stub
		
	}
	
//	public static void sign() {
//		String body = """
//				{
//					"operation": "TRANSFER",
//					"source": {
//						"type": "VAULT_ACCOUNT"
//					},
//					"destination": {
//						"type": "VAULT_ACCOUNT"
//					}
//				}""";
//		
//		Fireblocks fb = new Fireblocks();
//		fb.endpoint( "/v1/transactions");
//		fb.body( body);
//		fb.transact();
//	}
}
