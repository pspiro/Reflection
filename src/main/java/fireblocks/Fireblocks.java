package fireblocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Random;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.IStream;
import tw.util.S;
import util.StringHolder;

/** This shit works. You pass in everthing ahead of time, then call transact() */
public class Fireblocks {
	static String testPk = "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";
	static String prodPk = "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";
	static String testApi = "bbce654d-08da-2216-5b20-bed4deaad1be";
	static String prodApi = "e3f1c5b4-48f3-f495-7b14-23e729bc3628";

	static String prodBase = "BNB_BSC";
	static String testBase = "ETH_TEST3";
	static String platformBase;
	
	static Random rnd = new Random(System.currentTimeMillis());

	static String base = "https://api.fireblocks.io";
	
	private static String apiKey;
	private static String privateKey;
	
	private String operation;
	private String endpoint;
	private String body = "";  // optional
	
	public void operation(String v) { operation = v;	}
	public void body(String v) { body = v;	}
	public void endpoint(String v) { endpoint = v;	}
	
	/** Do not call this in production; for testing only. 
	 * @throws Exception */
	public static void setVals() throws Exception {
		Map<String, String> env = getEnv();
//		env.put( "api_key", prodApi);
//		env.put( "private_key", prodPk);
//		rusdAddress = Rusd.prodAddr;
//		platformBase = prodBase;

		env.put( "api_key", testApi);
		env.put( "private_key", testPk);
		platformBase = testBase;
		
		readKeys();		
	}

	public static void readKeys() throws Exception {
		apiKey = Util.getenv("api_key");
		privateKey = Util.getenv("private_key");
	}
	
	interface Ret {
		public void run(String ret); 
	}
	
	// return MyJsonObj
	String transact() throws Exception {
		S.out( "Sending Fireblocks transaction with body: %s", body);

		//S.out( "api key: %s", apiKey);

		long start = System.currentTimeMillis() / 1000;
		long expire = start + 29;

		// toJson removes spaces in the values, not good
		String bodyHash = Encrypt.getSHA(body);
		//S.out( "Body hash: %s", bodyHash);

		String header = toJson( "{ 'alg': 'RS256', 'typ': 'JWT' }");
		//S.out( "Header: %s", header);
		//S.out( "Encoded: %s", Encrypt.encode( header) );

		String nonce = String.valueOf( rnd.nextInt() );
		
		String payload = toJson( String.format( "{ "
				+ "'uri': '%s',"
				+ "'nonce': '%s',"
				+ "'iat': %s,"
				+ "'exp': %s,"
				+ "'sub': '%s',"
				+ "'bodyHash': '%s'"
				+ "}",
				endpoint, nonce, start, expire, apiKey, bodyHash) );
		//S.out( "Payload: %s", payload);
		//S.out( "Encoded: %s", Encrypt.encode( payload) );
		
		String input = String.format( "%s.%s",
				Encrypt.encode( header),
				Encrypt.encode( payload) );
		//S.out( "Input:");
		//System.out.println(input);
		
		String signed = Encrypt.signRSA( input, privateKey);
		//S.out( "Sig:");
		//System.out.println(signed);

		String jwt = String.format( "%s.%s.%s",
				Encrypt.encode( header), Encrypt.encode( payload), signed)
				.replace( "/", "_").replace( "+", "-");
		//System.out.println( jwt);
		
		//new OStream( "c:/temp/f2.t").write(body);
		
		StringHolder holder = new StringHolder();
		
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client.prepare(operation, base + endpoint)
			.setHeader("X-API-Key", apiKey)
			.setHeader("Connection", "close")
			.setHeader("Content-type", "application/json")
			.setHeader("Accept", "application/json, text/plain, */*")
			.setHeader("Authorization", "Bearer " + jwt)
			.setBody(body)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
		  			holder.val = obj.getResponseBody();
					//process(obj);
				}
				catch( Exception e) {
					e.printStackTrace();
				}
			}).join();
		
		return holder.val;
	}

	static void process(Response resp) throws Exception {
		String body = resp.getResponseBody();
		S.out( body);
		
		if (body.startsWith( "{") ) {
			MyJsonObject.parse(body).display();
		}
		else if (body.startsWith( "[")) {
			MyJsonArray.parse(body).display();
		}
	}

	static String toJson( String format, Object... params) {
		return String.format( format, params).replaceAll( "\\'", "\"").replaceAll( " ", "");
	}


	public static void rusdSell() {
		
	}

	private static Map<String, String> getEnv() throws Exception {
	    Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
	    Method getenv = pe.getDeclaredMethod("getenv", String.class);
	    getenv.setAccessible(true);
	    Field props = pe.getDeclaredField("theCaseInsensitiveEnvironment");
	    props.setAccessible(true);
	    return (Map<String, String>) props.get(null);
	}

	public static String get(String endpoint) throws Exception {
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		fb.operation( "GET");
		return fb.transact();
	}

	/** Round it to four decimal places and then convert to hex.
	 *  @param mult should be the 10^ number of decimal digits in the contract.  */
	public static String padDouble(double amt, BigDecimal mult) {
		BigInteger big = new BigDecimal( String.format( "%.4f", amt) )
				.multiply(mult)
				.toBigInteger();
		return padLeft( String.format( "%x", big) );
	}
	
	public static String padInt(int amt) {
		return padLeft( String.format( "%x", amt) );
	}
	
	/** Assume address starts with 0x */
	public static String padAddr(String addr) throws RefException {
		Main.require( addr != null && addr.length() == 42, RefCode.UNKNOWN, "Invalid address %s", addr);
		return padLeft( addr.substring(2) );
	}

	/** Pad with left zeros to 64 characters which is 32 bytes */
	private static String padLeft(String str) {
		return Util.padLeft( str, 64, '0'); 
	}
	
	private static String padRight(String str) {
		return Util.padRight( str, 64, '0'); 
	}
	

	/** For encoding parameters for deployment or contract calls.
	 *  Assume all string length <= 32 bytes 
	 * @throws RefException */
	public static String encodeParameters( String[] types, Object[] params) throws RefException {
		// no parameters passed?
		if (types == null) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		int statics = 0;

		// encode the parameters; strings just get a placeholder
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			if (type.equals("string") ) {
				Main.require( val instanceof String, RefCode.UNKNOWN, "Wrong type");
				
				String str = (String)val;
				Main.require( str.length() <= 32, RefCode.UNKNOWN, "String too long");
				
				// total number of parameters plus the number of strings (or other static types) that came before
				int num = (types.length + statics * 2) * 32;
				sb.append( padInt( num) );

				statics++;
			}
			else if (type.equals( "address") ) {
				sb.append( padAddr( (String)val) );
			}
			else if (type.equals( "uint256") ) {
				Util.require( val instanceof Integer, "Bad parameter type " + val.getClass() ); 
				sb.append( padInt( (Integer)val) );
			}
			else {
				Util.require( false, "Unknown type " + type);
			}
		}
		
		// encode the strings
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object val = params[i];
			if (type.equals("string") ) {
				sb.append( padInt( ((String)val).length() ) );
				String bytes = stringToBytes( (String)val);
				sb.append( padRight( bytes) );
			}
		}
		return sb.toString();
	}

	/** Return each byte's hex value */
	private static String stringToBytes(String val) {
		StringBuilder sb = new StringBuilder();
		for (byte b : val.getBytes() ) {
			sb.append( String.format( "%2x", b) );
		}
		return sb.toString();
	}
	
	/** Right now supporting only string non-static type. */
	private static boolean isStatic(String type) {
		return type.equals( "string") ? false : true;
	}

	public static MyJsonArray getTransactions() throws Exception {
		String str = get( "/v1/transactions");
		Util.require( str.startsWith( "["), "Error: " + str);
		return MyJsonArray.parse( str);
	}

	public static MyJsonObject getTransaction(String id) throws Exception {
		String str = get( "/v1/transactions/" + id);
		return toJsonObject( str);
	}
	
	public static MyJsonObject deploy(String filename, String[] paramTypes, Object[] params, String note) throws Exception {
		String data = new IStream(filename).readln();
		return call( "0x0", data, paramTypes, params, note); 
	}

	/** @param callData is keccak for call or bytecode for deploy; can start w/ 0x or not */
	public static MyJsonObject call(String addr, String callData, String[] paramTypes, Object[] params, String note) throws Exception {
		String bodyTemplate = 
				"{" + 
				"'operation': 'CONTRACT_CALL'," + 
				"'amount': '0'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '0'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"}," + 
				"'extraParameters': {" +
				"   'contractCallData': '%s'" +
				"}," +
				"'note': '%s'" + 
				"}";

		String fullCallData = callData + encodeParameters( paramTypes, params);  // call data + parameters 
		
		String body = toJson( 
				String.format( bodyTemplate, Fireblocks.platformBase, addr, fullCallData, note) );  

		Fireblocks fb = new Fireblocks();
		fb.endpoint( "/v1/transactions");
		fb.operation( "POST");
		fb.body( body);
		String ret = fb.transact();
		return toJsonObject( ret); 
	}
	
	private static MyJsonObject toJsonObject( String str) throws Exception {
		Util.require( str.startsWith( "{"), "Error: " + str);
		return MyJsonObject.parse( str);
	}
	
	String getAccounts() {
		return get("/v1/exchange_accounts");
	}
}
