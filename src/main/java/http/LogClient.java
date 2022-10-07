package http;

import java.io.IOException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import tw.util.S;

public class LogClient {
	public static void main(String[] args) throws IOException {

		AsyncHttpClient client = new DefaultAsyncHttpClient();
		client.prepare("GET", "https://deep-index.moralis.io/api/v2/0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48/logs?chain=eth&block_number=15696951")
		  .setHeader("accept", "application/json")
		  .setHeader("X-API-Key", "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6")
		  .execute()
		  .toCompletableFuture()
		  .thenAccept( obj -> doo(obj) ) //System.out::println)
		  .join();

		client.close();
	}

	private static void doo(Response obj) {
		S.out( obj.getResponseBody() );
	}
}

		
//	public static void mainn(String[] args) {
//		String host = "deep-index.moralis.io";
//		//host = "192.168.1.11";
//		int port = 443;
//		
//		try {
//			MyHttpClient client = new MyHttpClient( host, port);
//			client.header( "X-API-Key", "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6");
//			client.header( "accept", "application/json");
//			client.header( "host: deep-index.moralis.io");
//			client.header( "Upgrade-Insecure-Requests: 1");
//			client.header( "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36");
//			
//			
//			
//			client.get();
//			S.out( client.readString() );
////			client.get("api/v2/0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48/logs?chain=eth&block_number=15696961&topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//}
