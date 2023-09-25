package http;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.json.simple.JsonObject;

import common.Util.ExConsumer;
import common.Util.ObjectHolder;

// move this into another class
public class MyAsyncClient {
	public interface RetJson {
		void run(JsonObject str) throws Exception;
	}
	
	public static void getJson( String url, RetJson ret) {
		get( url, str -> ret.run( JsonObject.parse(str) ) );  
	}
	
	/** Returns response body */
	public static void get( String url, ExConsumer<String> ret) { 
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", url)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
					ret.accept(obj.getResponseBody() );
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
	}

	/** Returns the response body */
	public static String get( String url) {
		ObjectHolder<Response> holder = new ObjectHolder<>();

		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", url)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
					holder.val = obj;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}).join();
		
		return holder.val.getResponseBody();
	}
	
	public static void postToJson( String url, String body, RetJson ret) {
		post( url, body, str -> ret.run( JsonObject.parse(str) ) );
	}

	public static void post( String url, String body, ExConsumer<String> ret) { 
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("POST", url)
			.setBody(body)
			.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
				try {
					client.close();
					ret.accept(obj.getResponseBody() );
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
	}
}
