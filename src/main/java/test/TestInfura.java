package test;

import static java.lang.String.format;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import tw.util.S;
import util.StringHolder;

public class TestInfura {
	static String api = "bba0c8b8f46b4c6a8f740a74c6c4ad77";
	
	public static void main(String[] args) throws Exception {
		String url = format( "https://mainnet.infura.io/v3/%s", api);
	    String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\": [{\"from\": \"0xb60e8dd61c5d32be8058bb8eb970870f07233155\",\"to\": \"0xd46e8dd67c5d32be8058bb8eb970870f07244567\",\"gas\": \"0x76c0\",\"gasPrice\": \"0x9184e72a000\",\"value\": \"0x9184e72a\",\"data\": \"0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675\"}, \"latest\"],\"id\":2}";
	    
	    JsonObject obj = JsonObject.parse(data);
	    obj.display();
	    String ret = querySync( url, data);
	    S.out( "");
	    S.out( ret);
	    
	}		
	
	static String querySync(String url, String data) {
		StringHolder holder = new StringHolder();

	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client.prepare("POST", url)
			.setHeader("accept", "application/json")
			.setHeader("X-API-Key", "test")		
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			holder.val = obj.getResponseBody();
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}).join();  // the .join() makes is synchronous

		return holder.val;
	}
}
