package common;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JsonObject;

import http.MyClient;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.MyException;
import tw.util.S;

/** to get group chat id:
 *  add your bot to the group
 *  get updates for your bot: https://api.telegram.org/bot<YourBOTToken>/getUpdates
 *  
 *
 */
public class Telegram {
	static TimeZone zone = TimeZone.getTimeZone( "America/New_York" );
	
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey; 
	static final String chatId = "-1001262398926"; // community chat
	static final long D5 = Util.DAY * 5;
	
	//static final String chatId = "5053437013"; // ReflectionBot33

	public static void main(String[] args) {
		Util.executeEvery(0,  Util.MINUTE, () -> Util.wrap( () -> check() ) );
	}
	
	static void check() throws IOException, MyException, Exception {
		String today = Util.getDateFormatter( "E", zone).format( new Date() );
		String now = Util.getDateFormatter( "HH:mm", zone).format( new Date() );

		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, "Telegram").fetchRows() ) {
			String day = Util.left( row.getString("Day"), 3);
			String time = row.getString("Time");
			long sent = row.getLong("Sent");
			
			long interval = System.currentTimeMillis() - sent;
			
			if (day.equalsIgnoreCase( today) && now.compareTo( time) >= 0 && interval > D5) { 
				send( row.getString("Message") );
				row.setValue("Sent", "" + System.currentTimeMillis() );
				row.update();
			}
		}
	}
	
	static void send( String message) throws Exception {
		S.out( "Posting message " + message);
		
		JsonObject params = Util.toJson( 
				"chat_id", chatId,
				"text", message);
		S.out( params);

		HttpResponse<String> resp = MyClient.create(part1 + "/sendmessage", params.toString() )
				.header( "Content-Type", "application/json")
				.query();

		S.out( JsonObject.parse( resp.body() ) );
	}
}
