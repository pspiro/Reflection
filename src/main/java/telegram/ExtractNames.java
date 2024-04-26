package telegram;

import java.util.HashSet;

import org.json.simple.JsonObject;

import tw.util.S;

public class ExtractNames {
	static String text = """
			Hello, %s, this is Radar from Reflection, the stock token trading platform, writing to you on behalf of the Reflection team. 

			I have two important things to tell you:
				
			First, Reflection has increased the threshold for KYC to $600 effective immediately. Orders for an amount lower than the threshold will *not* trigger KYC. We hope this improves your trading experience.

			Second, we are looking to conduct interviews with users of the platform to get your feedback and suggestions. The interviews will be short, and you can earn $10 RUSD on Polygon which can be instantly redeemed for cash, if desired. If you would like to participate, please reply to this message.

			Thank you.""";


	public static void main(String[] args) throws Exception {
		HashSet<String> ids = new HashSet<>();
		
		send( "Peter Spiro", "user5053437013");
		
//		JsonObject.readFromFile( "c://temp//result.json")
//				.getArray( "messages").forEach( item -> {
//					if (item.getString("type").equals( "message") ) {
//						String name = item.getString("from");
//						String id = item.getString("from_id");
//						
//						if (!ids.contains( id)) {
//							ids.add( id);
//							send( name, id);
//							System.exit(0);
//						}
//					}
//				});
		
	}


	private static void send(String name, String id) throws Exception {
		Telegram.send( id, String.format( text, name) )
			.display();
	}
}
