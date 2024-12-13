package telegram;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

public class Chat {
	static String url = "https://api.openai.com/v1/chat/completions";
	static String apiKey = "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";

	static boolean isSpam(String text) throws Exception {
		Conv conv = new Conv();
		conv.sys( """
			You are moderating a Telegram group focused on Reflection, a stock token platform. Your task is to evaluate 
			each incoming message and determine whether it is spam. Messages that are relevant to stock trading, stock 
			tokens, or Reflection should be retained. All other messages should be considered spam and removed.
			
			If you are unsure, keep the message, to be safe.

			Your response should include two parts.
			Part 1: A single word on a line by itself: 'Keep' for messages that should remain, and 'Delete' for those that should be removed.
			Part 2: A one line explanation of why you selected to keep or remove the message.
			""");
	
		String str = conv.eval( text);
		S.out( str);
		return true;
		//Util.require( str.equals( "Keep") || str.equals( "Delete"), "Invalid response: " + str);
		
		//return str.equals( "Delete");
	}
	
	public static void main(String[] args) throws Exception {
		S.out( isSpam( "TON Tokens are now for sale, don't miss out") );
	}


	static class Conv extends JsonArray {
		
		public Conv() {
		}

		public Conv(Conv conv) {
			super( conv);
		}
		
		private Conv copy() {
			return new Conv( this);
		}

		private void sys( String text) {
			addItem( "system", text);
		}

		private void user( String text) {
			addItem( "user", text);
		}

		private void addItem( String role, String text) {
			add( Util.toJson( 
					"role", role,
					"content", text) );
		}
		
		/** evaluate new text, don't change conversation */ 
		String eval( String text) throws Exception {
			var conv = copy();
			conv.user( text);
			return conv.chat();
		}

		/** play what we got */
		void play() throws Exception {
			S.out( chat() );
		}
		
		private String chat() throws Exception {
			var body = Util.toJson(
					"model", "gpt-4",
					"messages", this,
					"max_tokens", 150,
					"temperature", .50 // Control creativity
					);
			//body.display();
			var resp = MyClient.create( url, body.toString() )
				.header( "Authorization", "Bearer " + apiKey)
				.header( "Content-Type", "application/json")
				.queryToJson();
//			resp.display();
			return resp.getArray( "choices").get( 0).getObject( "message").getString( "content").trim();
		}
	}
	
}
