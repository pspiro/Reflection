package http;

import java.util.Random;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import json.MyJsonObject;
import tw.util.S;

/** This shit works. */
public class Fireblocks {
	static Random rnd = new Random(System.currentTimeMillis());

	static String base = "https://api.fireblocks.io";
	
	private String apiKey;
	private String privateKey;
	private String operation;
	private String endpoint;
	private String body = "";  // optional
	
	// return MyJsonObj
	void transact() throws Exception {
		S.out( "api key: %s", apiKey);

		long start = System.currentTimeMillis() / 1000;
		long expire = start + 29;

		// toJson removes strings in the values, not good
		String bodyHash = Encrypt.getSHA(body);
		S.out( "Body: %s", body);
		S.out( "Body hash: %s", bodyHash);

		String header = toJson( "{ 'alg': 'RS256', 'typ': 'JWT' }");
		S.out( "Header: %s", header);
		S.out( "Encoded: %s", Encrypt.encode( header) );

		String nonce = String.valueOf( rnd.nextInt() );
		
		String payload = toJson( String.format( "{ 'uri': '%s', 'nonce': '%s', 'iat': %s, 'exp': %s, 'sub': '%s', 'bodyHash': '%s' }",
				endpoint, nonce, start, expire, apiKey, bodyHash) );
		S.out( "Payload: %s", payload);
		S.out( "Encoded: %s", Encrypt.encode( payload) );
		
		String input = String.format( "%s.%s",
				Encrypt.encode( header),
				Encrypt.encode( payload) );
		S.out( "Input:");
		System.out.println(input);
		
		String signed = Encrypt.signRSA( input, privateKey);
		S.out( "Sig:");
		System.out.println(signed);

		String jwt = String.format( "%s.%s.%s",
				Encrypt.encode( header),
				Encrypt.encode( payload),
				signed);

		S.out( "JWT:");
		System.out.println( jwt);
		jwt = jwt.replace( "/", "_").replace( "+", "-");
		System.out.println( jwt);
		
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
					process(obj);
				}
				catch( Exception e) {
					e.printStackTrace();
				}
			}); //.join();
	}

	static void process(Response obj) {
		S.out( obj.getResponseBody() );
	}

	static String toJson( String str) {
		return str.replaceAll( "\\'", "\"").replaceAll( " ", "");
	}

	public void apiKey(String v) {
		apiKey = v;
	}

	public void privateKey(String v) {
		privateKey = v;
	}

	public void operation(String v) {
		operation = v;
	}

	public void body(String v) {
		body = v;
	}

	public void endpoint(String v) {
		endpoint = v;
	}
}
