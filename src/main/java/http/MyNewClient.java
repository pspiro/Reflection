package http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import test.MyTimer;
import tw.util.S;

public class MyNewClient {
	public static void main(String[] args) throws Throwable {
		MyTimer t = new MyTimer();
		t.next("test1");
		for (int i = 0; i < 10; i++) {
			test1();
		}
		t.next("test2");
		for (int i = 0; i < 10; i++) {
			test2();
		}
		t.next("test3");
		for (int i = 0; i < 10; i++) {
			test3();
		}
		t.done();
	}
	
	static void test1() throws Throwable {
		S.out( MyAsyncClient.get("https://reflection.trading/api/ok") );
	}
	
	static void test2() {
		HttpClient client = HttpClient.newBuilder().build();

		// Define the URI for the HTTPS request
		URI uri = URI.create("https://reflection.trading/api/ok");

		// Create an HttpRequest for a GET request to the URI
		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.GET()
				.build();

		// Send the request asynchronously and handle the response
		CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.thenAccept(responseBody -> {
					S.out(responseBody);
				})
				.exceptionally(ex -> {
					System.err.println("Request failed: " + ex.getMessage());
					return null;
				});

		// Wait for the request to complete (in a real application, you may want to use more robust handling)
		future.join();
	}
	
	static void test3() throws Exception {
		// Create an HttpRequest for a GET request to the URI
		HttpRequest request = HttpRequest.newBuilder()
				.uri( URI.create("https://reflection.trading/api/ok") )
				.GET()
				.build();

		// Send the request asynchronously and handle the response
		HttpResponse<String> res = HttpClient.newBuilder().build()
				.send(request, HttpResponse.BodyHandlers.ofString());
		
		S.out(res.body() );
	}
}
