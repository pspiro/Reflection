package telegram;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JsonObject;

import common.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.MyException;
import tw.util.S;

/** to get group chat id:
 *  add your bot to the group
 *  get updates for your bot: https://api.telegram.org/bot<YourBOTToken>/getUpdates
 *  
 *  you are very smart
 *  you know everything there is to know about Reflection
 *  you are kind, you want to make people feel good about themselves
 *  you are help, and you want to be helpful
 *  you are here to serve people, to give them the information they need, and to brighten their day, if you can
 *
 */
public class TgServer {
	static TimeZone zone = TimeZone.getTimeZone( "America/New_York" );
	static final String ReflectionCommunity = "-1001262398926"; // community chat
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey; 
	static final String peterSpiro = "5053437013";

	static final long D5 = Util.DAY * 5;
	
	//static final String chatId = "5053437013"; // ReflectionBot33

	// https://core.telegram.org/bots/api#available-methods
	public static void main(String[] args) throws Exception {
		Util.executeEvery("Telegram", 0,  Util.MINUTE, () -> Util.wrap( () -> check() ) );
		//queryMessages();
		
//		String url = String.format( "https://api.telegram.org/%s/getChatMember?chat_id=%s&user_id=%s", 
//				botKey, communityChatId, peterSpiro);
//		MyClient.getJson(url).display();
	}
	
	/** Listen for messages sent to my group or to the bot */
//	private static void queryMessages() throws Exception {
//		int last = 271551453;  // query for id's with this number or higher
//		
//		while (true) {
//			String url = String.format( "https://api.telegram.org/%s/getUpdates?timeout=600&limit=30&offset=%s", 
//					Telegram.botKey, last + 1);
//
//			for (JsonObject update : MyClient.getJson(url).getArray("result") ) {
//				last = processUpdate( update);
//			}
//			
//			S.sleep(10);
//		}
//	}
	
	static int processUpdate( JsonObject item) throws Exception {
		int updateId = item.getInt( "update_id");
		JsonObject msg = item.getObject( "message");
		String msgId = msg.getString( "message_id");
		JsonObject from = msg.getObject( "from");		// id, is_bot, first_name, username
		JsonObject chat = msg.getObject( "chat");      // id, title, username, type
		String time = Util.yToS.format( msg.getLong( "date") * 1000 );
		String text = msg.getString( "text");
		
		from.remove( "language_code");
		from.remove( "is_premium");
		
		S.out( "\ntime:%s  \nmsgId:%s  \nfrom:%s  \nin:%s\n%s", 
				time, msgId, from, chat, text);
		
		if (from.getString("id").equals(peterSpiro) ) {
			S.out( "deleting message");
			Telegram.deleteMessage( chat.getString("id"), msgId);
		}
		
		return updateId;
	}
	
	static void check() throws IOException, MyException, Exception {
		String today = Util.getDateFormatter( "E", zone).format( new Date() );
		String now = Util.getDateFormatter( "HH:mm", zone).format( new Date() );

		for (ListEntry row : NewSheet.getTab( NewSheet.Telegram, "Telegram").fetchRows() ) {
			String day = Util.left( row.getString("Day"), 3);
			String time = adjust( row.getString("Time") );
			long sent = row.getLong("Sent");
			
			long interval = System.currentTimeMillis() - sent;
			
			if (day.equalsIgnoreCase( today) && now.compareTo( time) >= 0 && interval > D5) { 
				send( row.getString("Message") );
				row.setValue("Sent", "" + System.currentTimeMillis() );
				row.update();
			}
		}
	}
	
	/** return HH:MM */
	private static String adjust(String time) {
		return time.length() < 5 ? "0" + time : time;
	}

	static void send( String message) throws Exception {
		send( message, ReflectionCommunity);
	}
	
	static void send( String message, String chatId) throws Exception {
		S.out( "Posting message " + message);
		S.out( Telegram.send( ReflectionCommunity, message) );
	}
}

//<dependency>
//    <groupId>org.telegram</groupId>
//    <artifactId>telegrambots</artifactId>
//    <version>6.0.1</version>
//</dependency>
	