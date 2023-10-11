package prefinery;

import org.json.simple.JsonObject;

import http.MyClient;

// not used
public class Prefinery {
	public static void main(String[] args) throws Throwable {
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
		
		String body = MyClient.create(url, json)
					.header("Content-Type", "application/json")
					.query()
					.body();
		out(body);
	}

	private static void out(String responseBody) throws Exception {
		JsonObject.parse(responseBody).display();
	}
}
