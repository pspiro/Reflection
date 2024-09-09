package telegram;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

public class Telegram {
	static final String reflectionChatId = "-1001262398926"; // community chat
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey;
	
	public static void main(String[] args) throws Exception {
		//post( "Visit <a href=\"https://reflection.trading\">Reflection</a>");
		post( "Visit *it* [Reflection](https://reflection.trading)");
	}

	/** post to Reflection channel; format for links is [text](link) */
	public static void post( String message) {
		Util.wrap( () -> send( reflectionChatId, message) );
	}
	
	/** send message with web preview disabled */
	static JsonObject send( String chatId, String message) throws Exception {
		S.out( "Posting to Telegram: " + message);
		
		JsonObject params = Util.toJson( 
				"chat_id", chatId,
				"text", message,
				"disable_web_page_preview", true,
				"parse_mode", "Markdown");  // or "HTML"
		S.out( params);

		return MyClient.create(part1 + "/sendmessage", params.toString() )
				.header( "Content-Type", "application/json")
				.queryToJson();
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
	
	static void queryMessages( String chatId) throws Exception {
		int last = 1;  // query for id's with this number or higher

		while (true) {
			String url = String.format( "https://api.telegram.org/%s/getUpdates?timeout=600&limit=30&offset=%s", 
					chatId, last + 1);

			for (JsonObject update : MyClient.getJson(url).getArray("result") ) {
				if (update != null) {
					last = processUpdate( update);
				}
			}

			S.sleep(10);
			break;
		}
	}
	
	static int processUpdate( JsonObject item) throws Exception {
		int updateId = item.getInt( "update_id");
		JsonObject msg = item.getObject( "message");
		JsonObject from = msg.getObject( "from");		// id, is_bot, first_name, username
		
		from.remove( "language_code");
		from.remove( "is_premium");
		
		S.out( item);
		
		return updateId;
	}
	

}
