package telegram;

import java.net.http.HttpResponse;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

public class Telegram {
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey; 

	static JsonObject send( String chatId, String message) throws Exception {
		S.out( "Sending to " + chatId);
		S.out( "Posting message " + message);
		
		JsonObject params = Util.toJson( 
				"chat_id", chatId,
				"text", message);
		S.out( params);

		HttpResponse<String> resp = MyClient.create(part1 + "/sendmessage", params.toString() )
				.header( "Content-Type", "application/json")
				.query();

		return JsonObject.parse( resp.body() );
	}

	public static void getMember(String chatId, String userId) throws Exception {
		String url = String.format( "https://api.telegram.org/%s/getChatMember?chat_id=%s&user_id=%s", 
				botKey, chatId, userId);
		MyClient.getJson(url).display();
	}
	
	static void deleteMessage(String chatId, String msgId) throws Exception {
		String url = String.format( "https://api.telegram.org/%s/deleteMessage?chat_id=%s&message_id=%s",
				botKey, chatId, msgId);
		MyClient.getJson( url).display();
	}
	
}
