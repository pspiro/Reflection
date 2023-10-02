package http;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExConsumer;
import common.Util.ObjectHolder;
import tw.util.S;

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
			.whenComplete( (obj, e) -> {
				if (obj != null) {
					Util.wrap( () -> ret.accept(obj.getResponseBody() ) );
				}
				else {
					S.out( "Error: could not get url " + url);  // we need this because the stack trace does not indicate where the error occurred
					if (e != null) {
						e.printStackTrace();
					}
				}
				Util.wrap( () -> client.close() );
			});
	}

	/** Returns the response body 
	 * @throws Throwable */
	public static String get( String url) throws Throwable {
		ObjectHolder<Response> objHolder = new ObjectHolder<>();
		ObjectHolder<Throwable> exHolder = new ObjectHolder<>();
		
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", url)
			.execute()
			.toCompletableFuture()
			.whenComplete( (obj, e) -> {     // e is actually type Throwable
				objHolder.val = obj;
				exHolder.val = e;
				
				if (obj == null) {
					S.out( "Error: could not get url " + url);  // we need this because the stack trace does not indicate where the error occurred
					// it will be up to the caller to handle the exception
				}
				Util.wrap( () -> client.close() );
			})
			.join();
		
		if (exHolder.val != null) {
			throw exHolder.val;
		}
		
		return objHolder.val.getResponseBody();
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
