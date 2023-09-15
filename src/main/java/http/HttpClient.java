package http;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import common.Util;

public class HttpClient {
	private Response m_obj;
	
	public String getBody() {
		return m_obj.getResponseBody();
	}
	
	public int getStatusCode() {
		return m_obj.getStatusCode();
	}

	public void post(String url, String body) throws Exception {
	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("POST", url)
			.setHeader("accept", "application/json")
			.setHeader("content-type", "application/json")
			.setBody(body)
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			m_obj = obj;
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}).join();  // the .join() makes it synchronous
		
		Util.require( getStatusCode() == 200, "Error status code %s - %s", getStatusCode(), getBody() );
	}

	public void get(String url) throws Exception {
	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", url)
			.setHeader("accept", "application/json")
			.setHeader("content-type", "application/json")
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			m_obj = obj;
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}).join();  // the .join() makes it synchronous
		
		Util.require( getStatusCode() == 200, "Error status code %s - %s", getStatusCode(), getBody() );
	}
}
