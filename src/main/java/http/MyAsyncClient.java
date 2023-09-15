package http;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

// move this into another class
public class MyAsyncClient {
	public interface Ret {
		void run(String str) throws Exception;
	}

	public interface RetJson {
		void run(JsonObject str) throws Exception;
	}
	
	public static void getJson( String url, RetJson ret) {
		get( url, str -> ret.run( JsonObject.parse(str) ) );  
	}
	
	public static void get( String url, Ret ret) { 
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", url)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
					ret.run(obj.getResponseBody() );
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
	}
	
	public static void postToJson( String url, String body, RetJson ret) {
		post( url, body, str -> ret.run( JsonObject.parse(str) ) );
	}

	public static void post( String url, String body, Ret ret) { 
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("POST", url)
			.setBody(body)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
					ret.run(obj.getResponseBody() );
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
	}
}
