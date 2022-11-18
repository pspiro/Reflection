package proxy;

import java.io.IOException;
import java.util.Map.Entry;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import http.SimpleTransaction;
import io.netty.handler.codec.http.HttpHeaders;
import tw.util.S;

public class Proxy {
	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.valueOf( args[1]);
		String serverUrl = args[2];
		
		S.out( "Listening on %s:%s  ---> %s", host, port, serverUrl);
		SimpleTransaction.listen(host, port, trans -> {
			try {
				patch( trans.exchange(), serverUrl);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} );
	}

	static int TENK = 1024 * 10;
	
	private static void patch(HttpExchange client, String serverUrl) throws IOException {
	    AsyncHttpClient server = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
	    
	    byte[] data = new byte[TENK];
	    client.getRequestBody().read( data);
	    
	    String fullUrl = serverUrl + client.getRequestURI();

	    S.out( "");
	    S.out( "CLIENT --------> SERVER %s", fullUrl);
	    S.out( client.getRequestURI() );
	    out( client.getRequestHeaders() );
	    if (data.length > 0) S.out( new String(data) );

	    BoundRequestBuilder msgToServer = server.prepare(client.getRequestMethod(), fullUrl);
		msgToServer.setHeaders( client.getRequestHeaders() );
		msgToServer.setBody( data);
		
		msgToServer.execute()
			.toCompletableFuture()
			.thenAccept( obj -> {
		  		try {
		  			byte[] bytes = obj.getResponseBodyAsBytes();
		  			
		  			S.out( "");
		  			S.out( "CLIENT <-------- SERVER");
		  			out( obj.getHeaders() );
					if (bytes.length > 0) S.out( new String(bytes) );

		  			// send headers from server to client
		  			for (Entry<String, String> header : obj.getHeaders() ) {
		  				client.getResponseHeaders().add( header.getKey(), header.getValue() );
		  			}
					client.sendResponseHeaders(200, bytes.length);

					// send body from server to client
					client.getResponseBody().write(bytes);
					client.getResponseBody().close();
					server.close();
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	});
	}


	private static void out(Headers headers) {
		for (String tag : headers.keySet() ) {
			for (String val : headers.get( tag) ) {
				S.out( "%s: %s", tag, val);
			}
		}
	}

	private static void out(HttpHeaders headers) {
		for (Entry<String, String> header : headers) {
			S.out( "%s: %s", header.getKey(), header.getValue() );
		}
	}
}
