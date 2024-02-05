package common;

import java.net.http.HttpResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JsonObject;

import http.MyClient;
import tw.util.S;

public class Telegram {
	static final String botKey = "bot6642832599:AAF8J9ymAXIfyLZ6G0UcU2xsU8_uHhpSXBY";
	static final String part1 = "https://api.telegram.org/" + botKey; 
	static final String chatId = "-1001262398926"; // community chat
	//static final String chatId = "5053437013"; // ReflectionBot33

	public static void main(String[] args) throws Exception {
		send( "Hi, I am a bot and I am learning to talk :)");
	}

	static void send( String message) throws Exception {
		JsonObject params = Util.toJson( 
				"chat_id", chatId,
				"text", message);
		S.out( params);

		HttpResponse<String> resp = MyClient.create(part1 + "/sendmessage", params.toString() )
				.header( "Content-Type", "application/json")
				.query();

		JsonObject ret = JsonObject.parse( resp.body() );
		ret.display();

		S.sleep(10000);
	}
}
