package telegram;

import java.net.http.HttpResponse;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

// use @userinfobot to get your chat id
public class TelegramTest {
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey; 
	static final String reflectionChatId = "-1001262398926"; // community chat
	static final String peterSpiro = "5053437013";
	static final String botId = "5053437013"; // ReflectionBot33
	
	// https://core.telegram.org/bots/api#available-methods
	public static void main(String[] args) throws Exception {
		//queryMessages();
		TgServer.send( "abc", "5053437013");
	}
	
	/* sample message; you can get the user id's here so you can send them messages from the bot
	{
	"update_id":271551678,
	"message":
		{
		"date":1711952986,
		"chat":{"id":-1001262398926, "title":"Reflection Community", "type":"supergroup", "username":"ReflectionTrading"},
		"message_thread_id":3234,
		"message_id":3235,
		"from":{"last_name":"Benny", "id":5997797697, "is_bot":false, "first_name":"Richard", "username":"Richardbenny"},
		"text":"I agree"
		}
	}
*/
	
//	private static void queryMessages() throws Exception {
//		int last = 1;  // query for id's with this number or higher
//		
//		while (true) {
//			String url = String.format( "https://api.telegram.org/%s/getUpdates?timeout=600&limit=30&offset=%s", 
//					botKey, last + 1);
//
//			for (JsonObject update : MyClient.getJson(url).getArray("result") ) {
//				last = processUpdate( update);
//			}
//			
//			S.sleep(10);
//			break;
//		}
//	}
	
	static int processUpdate( JsonObject item) throws Exception {
		int updateId = item.getInt( "update_id");
		JsonObject msg = item.getObject( "message");
		JsonObject from = msg.getObject( "from");		// id, is_bot, first_name, username
		
		from.remove( "language_code");
		from.remove( "is_premium");
		
		S.out( item);
		
		return updateId;
	}
	
	static void send( String chatId, String message) throws Exception {
		S.out( "Sending to %s: %s", chatId, message);
		
		JsonObject params = Util.toJson( 
				"chat_id", chatId,
				"text", message);
		S.out( params);

		JsonObject resp = MyClient.create(part1 + "/sendmessage", params.toString() )
				.header( "Content-Type", "application/json")
				.queryToJson();

		S.out( resp);
	}
}
