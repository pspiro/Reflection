package common;

import http.MyClient;
import tw.util.S;

/** Chat-GPT client */
public class ChatGptClient {
	static String key = "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";
	static String url = "https://api.openai.com/v1/chat/completions";
	
	public static void main(String[] args) throws Exception {
		
		String json = """
			{
			"model": "gpt-3.5-turbo",
			"messages": [
				{
				"role": "system",
				"content": "You are a poetic assistant, skilled in explaining complex programming concepts with creative flair."
				},
				{
				"role": "user",
				"content": "Compose a poem that explains the concept of recursion in programming."
				}
			]
			}""";
		
		String resp = MyClient.create( url, json.toString() )
			.header("Content-Type", "application/json")
			.header( "Authorization", "Bearer " + key)
			.query().body();
		S.out( resp);
		
//		MyClient.postToJson( , json)
//			.display();
			//-H "Content-Type: application/json"   
		// -H "Authorization: Bearer $OPENAI_API_KEY"   -d '{
		
	}
}
