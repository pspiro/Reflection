package telegram;

import java.util.HashSet;

import org.json.simple.JsonObject;

import common.Util;

public class ExtractNames {
	static String text = """
Hi, %s, this message is from Peter Spiro, Founder and CEO of Reflection.

As you know, Reflection is a platform where you can trade tokenized stocks such as Apple, Google, and Amazon. I see that you are an active member of our Telegram community, but not active on the Reflection platform. If I may ask—why is that? Is there anything in particular that prevents you from trading on Reflection? Are there any features you would like to see added that would entice you to start trading?

I really appreciate your feedback.

Peter Spiro
Founder and CEO, Reflection""";
	static int i = 0;

	public static void main(String[] args) throws Exception {
		HashSet<String> ids = new HashSet<>();
		
		//send( "Peter Spiro", "user5053437013");
		
		
		JsonObject.readFromFile( "c://temp//result.json")
				.getArray( "messages").forEach( item -> {
					if (item.getString("type").equals( "message") ) {
						String name = item.getString("from");
						String id = item.getString("from_id");
						
						if (!ids.contains( id)) {
							ids.add( id);
							Util.wrap( () -> send( name, id) );
							if (++i == 5) {
								System.exit(0);
							}
						}
					}
				});
		
	}


	private static void send(String name, String id) throws Exception {
		if (id.startsWith( "user") ) {
			id = id.substring(4);
		}
		
		Telegram.send( id, String.format( text, name) )
			.display();
	}
}
