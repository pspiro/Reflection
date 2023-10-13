package http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExConsumer;
import tw.util.S;

/** Client for all HttpRequests */
public class MyClient {
	Builder m_builder;
	
	/** build GET request; call this directly to add headers */
	public static MyClient create(String url) {
		return new MyClient( HttpRequest.newBuilder()
				.uri( URI.create(url) )
				.GET() );
	}
		
	/** build POST request; call this directly to add headers */
	public static MyClient create(String url, String body) {
		return new MyClient( HttpRequest.newBuilder()
				.uri( URI.create(url) )
				.POST(HttpRequest.BodyPublishers.ofString(body)));
	}
	

	MyClient( Builder builder) {
		m_builder = builder;
	}

	public MyClient header( String tag, String val) {
		m_builder.header( tag, val);
		return this;
	}

	/** query and return response */
	public HttpResponse<String> query() throws Exception {
		try {
			return HttpClient.newBuilder().build()
					.send(m_builder.build(), HttpResponse.BodyHandlers.ofString());
		}
		catch( Throwable e) {
			throw (Exception)e; // check the type. pas 
		}
	}
	
	/** async query; WARNING: the response comes in a daemon thread which will not keep
	 *  the program alive if there are no other threads */
	public void query(ExConsumer<HttpResponse<String>> ret) {
		HttpRequest request = m_builder.build();
		
		HttpClient.newBuilder().build().sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenAccept(response -> {
					Util.wrap( () -> ret.accept(response) );
				})
				.exceptionally(ex -> {
					S.out( "Error: could not get url %s - %s", request.uri(), ex.getMessage() );  // we need this because the stack trace does not indicate where the error occurred
					ex.printStackTrace();
					return null;
				});
		
//		Util.execute( () -> S.sleep(5000) );
	}
	
	// ----- synchronous helper methods ----------------------------

	public static JsonObject getJson(String url) throws Exception {
		return JsonObject.parse( getString(url) );
	}

	public static String getString( String url) throws Exception {
		return getResponse(url).body();
	}

	public static HttpResponse<String> getResponse(String url) throws Exception {
		return create(url).query();
	}


	// ----- asynchronous helper methods ----------------------------
	
	/** get string, async */
	public static void getString( String url, ExConsumer<String> ret) {
		create(url).query( resp -> ret.accept( resp.body() ) );
	}
	
	/** get json object, async */
	public static void getJson( String url, ExConsumer<JsonObject> handler) {
		create(url).query( resp -> handler.accept( JsonObject.parse(resp.body() ) ) );
	}

	/** get json array, async */
	public static void getArray( String url, ExConsumer<JsonArray> handler) {
		getString(url, body -> handler.accept( JsonArray.parse(body) ) );
	}

	/** post to json object, async */
	public static void postToJson( String url, String body, ExConsumer<JsonObject> ret) {
		create( url, body).query( resp -> ret.accept( JsonObject.parse(resp.body() ) ) );
	}

	
	/** send synchronous request, return full response */
}
