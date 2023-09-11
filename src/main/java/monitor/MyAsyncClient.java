package monitor;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

// move this into another class
public class MyAsyncClient {
	interface Ret {
		void run(String str) throws Exception;
	}
	
	static void get( String url, Ret ret) { 
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
}
