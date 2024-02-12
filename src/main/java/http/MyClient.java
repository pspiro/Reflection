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
import tw.util.OStream;
import tw.util.S;

/** Client for all HttpRequests */
public class MyClient {
	static final String filename = "http.log";
	
	static HttpClient client = HttpClient.newBuilder().build();

	private Builder m_builder;
	
	private static void write( String line) {
		try ( OStream os = new OStream( filename, true) ) {
			os.writeln( line);
		}
		catch( Exception e) {
			// ignore it
		}
	}
	
	/** build GET request; call this directly to add headers */
	public static MyClient create( String url) {
		write( url + " GET");
		return new MyClient( reqBuilder( url).GET() );
	}
		
	/** build POST request; call this directly to add headers */
	public static MyClient create( String url, String body) {
		write( url + " POST");
		return new MyClient( reqBuilder( url).POST( HttpRequest.BodyPublishers.ofString( body)));
	}

	/** Create PUT */
	public static MyClient createPut( String url, String body) {
		write( url + " PUT");
		return new MyClient( reqBuilder( url)
				.PUT( HttpRequest.BodyPublishers.ofString( body)));
	}

	/** Create DELETE */
	public static MyClient createDelete( String url) {
		write( url + " DELETE");
		return new MyClient( reqBuilder( url).DELETE() );
	}

	/** Create Builder with URL */
	private static Builder reqBuilder( String url) {
		return HttpRequest.newBuilder().uri( URI.create( url) );
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
			HttpRequest request = m_builder.build();

			HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString());

			// avoid returning html messages from nginx; at least catch 404 and 502 
			if (!niceCode( response.statusCode() ) ) {
				throw new ClientException( response.statusCode(), response.body(), request.uri() );
			}
			
			return response;
		}
		catch( Throwable e) {
			throw ( Util.toException( e) ); // check the type. pas 
		}
	}
	
	/** async query; WARNING: the response comes in a daemon thread which will not keep
	 *  the program alive if there are no other threads */
	public void query( ExConsumer<HttpResponse<String>> ret) {
		HttpRequest request = m_builder.build();
		
		HttpClient.newBuilder().build().sendAsync( request, HttpResponse.BodyHandlers.ofString())
				.thenAccept( response -> { 
						if (niceCode( response.statusCode() ) ) {  // catch 502 and 404 here
							try {
								ret.accept( response);
							}
							catch( Exception e) {
								S.out( "Error processing url %s", request.uri() );
								e.printStackTrace();
							}
						}
						else {
							// if we want, we could pass in the exception with the call stack and throw it here
							S.out( "Error: received status code %s fetching URL %s", response.statusCode(), request.uri() );
						}
				})
				.exceptionally( ex -> {
					S.out( "Error: could not get url %s - %s", request.uri(), ex.getMessage() );  // we need this because the stack trace does not indicate where the error occurred
					// ex.printStackTrace(); this stack trace is useless because it does not contain any of our functions
					return null;
				});
	}
	
	/** Really we want to at least catch 404 and 502 */
	private static boolean niceCode( int statusCode) {
		return statusCode == 200 || statusCode == 400; // 400 is returned by RefAPI along w/ an error message
	}
	
	// ----- synchronous helper methods - get ----------------------------

	/** get json object */ 
	public static JsonObject getJson( String url) throws Exception {
		return JsonObject.parse( getString( url) );
	}

	/** get json array */ 
	public static JsonArray getArray( String url) throws Exception {
		return JsonArray.parse( getString( url) );
	}

	/** get string
	 *  For 502 errors, the Http client will throw an exception. 
	 *  For 404, it does not, but we want it to */
	public static String getString( String url) throws Exception {
		return getResponse( url).body();
	}

	/** get repsonse */
	public static HttpResponse<String> getResponse( String url) throws Exception {
		return create( url).query();
	}
	
	// ----- synchronous helper methods - post ----------------------------
	
	/** post to json */
	public static JsonObject postToJson( String url, String body) throws Exception {
		return JsonObject.parse( postToString( url, body) );
	}

	/** post to string 
	 * @throws Exception */
	public static String postToString( String url, String body) throws Exception {
		return postToResponse( url, body).body();
	}

	/** post to response 
	 * @return 
	 * @throws Exception */
	public static HttpResponse<String> postToResponse( String url, String body) throws Exception {
		return create( url, body).query();
	}



	// ----- asynchronous helper methods ----------------------------
	// these messages will not keep the program alive and will not even send
	// if the program is allowed to terminate, so not good for testing
	
	/** get string, async 
	 *  Note that this will NOT keep the program alive*/
	public static void getString( String url, ExConsumer<String> ret) {
		create( url).query( resp -> ret.accept( resp.body() ) );
	}
	
	/** get json object, async 
	 *  Note that this will NOT keep the program alive*/
	public static void getJson( String url, ExConsumer<JsonObject> handler) {
		create( url).query( resp -> handler.accept( JsonObject.parse( resp.body() ) ) );
	}

	/** get json array, async
	 *  Note that this will NOT keep the program alive */
	public static void getArray( String url, ExConsumer<JsonArray> handler) {
		getString( url, body -> handler.accept( JsonArray.parse( body) ) );
	}

	/** post to json object, async
	 *  Note that this will NOT keep the program alive */
	public static void postToJson( String url, String body, ExConsumer<JsonObject> ret) {
		create( url, body).query( resp -> ret.accept( JsonObject.parse( resp.body() ) ) );
	}

	
	/** send synchronous request, return full response */
}
