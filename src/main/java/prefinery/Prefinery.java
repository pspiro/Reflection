package prefinery;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

// not used
public class Prefinery {
	public static void main(String[] args) {
		String url = "/api/v2/betas/{beta_id}/testers";
		
		String json = """
		{
			"tester" : {
				"email": "bruce@wayneenterprises.com",
				"status": "applied",
				"profile": {
					"first_name": "Bruce",
					"last_name": "Wayne",
				},
				"responses": {
					"response":
					[
						{
							"question_id": "23874",
							"answer": "a text response"
						},
						{
							"question_id": "23871",
							"answer": "1"
						},
						{
							"question_id": "23872",
							"answer": "0,2"
						},
						{
							"question_id": "23873",
							"answer": "9"
						}
					]
				}
			}
		} """;
		
		AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("POST", url)
			.setHeader("Content-Type", "application/json")
			.setBody(json)
			.execute()
			.toCompletableFuture()
			.whenComplete( (obj, e) -> {
				if (obj != null) {
					Util.wrap( () -> out( obj.getResponseBody() ) );
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

	private static void out(String responseBody) throws Exception {
		JsonObject.parse(responseBody).display();
		
	}
}
