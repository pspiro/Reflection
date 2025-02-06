package telegram;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;
import tw.util.S;

/*
Using the Telegram API, write a program that does the following:
Monitor a specific chat for new messages.
When a new message appears, use the ChatGPT API to evaluate if it is spam.
If it is spam, delete it and write to standard out that the message has been deleted. 
Also, any post that is just a number from 1 to 100, delete that as well. (These are part of the captcha for new members) 
 */
public class TgAi {
	// anything with MAJOR in the subject
	
    private static final String TgUrl = TgServer.url();
    private static final String deleteMessageUrl = TgUrl + "/deleteMessage";
    
    private static final String CHATGPT_API_KEY = "your-chatgpt-api-key";
    private static final String CHATGPT_API_URL = "https://api.openai.com/v1/chat/completions";
    
//    String bad = """
//    		
//    		"""

    public static void main(String[] args) {
        long lastUpdateId = 0;

        while (true) {
            try {
                // Get updates from Telegram
                String getUpdatesUrl = TgUrl + "/getUpdates?offset=" + (lastUpdateId + 1);
                JsonObject updatesJson = MyClient.getJson( getUpdatesUrl);

                if (updatesJson.getBool("ok")) {
                    for (var update : updatesJson.getArray("result") ) {
                    	S.out( update);
                        lastUpdateId = update.getLong("update_id");

                        if (update.has("message")) {
                            JsonObject message = update.getObject("message");
                            message.update( "date", date -> Util.yToS.format( (long)date * 1000) );
                            message.remove( "entities");
                            message.remove( "caption_entities");
                            
                            String chatId = message.getObject("chat").getString("id");
                            String messageId = message.getString("message_id");
                            String text = message.getString("text");
                            

                            // Check if the message is spam or a number between 1 and 100
                            if (isSpam(message) || isNumberBetween1And100(text)) {
                                Telegram.deleteMessage(chatId, messageId);
                                S.out("Deleted message: " + text);
                            }
                            else {
                            	S.out( "Received:");
                            	message.display();
                            }
                        }
                    }
                }

                // Sleep to avoid hitting rate limits
                Thread.sleep(10000);
            } catch (Exception e) {
                //e.printStackTrace();
            	S.out( e.getMessage() );
            }
        }
    }

    private static boolean isSpam(JsonObject message) throws Exception {
        String caption = message.getString( "caption").toUpperCase();
        String forName = message.getString( "forward_sender_name").toUpperCase();
        String text = message.getString( "text").toUpperCase();

        if (message.getObject( "story") != null) {
    		return true;
    	}
        
        if (Util.equals( forName, "TON SPIN") ) {
        	return true;
        }
        
        if (caption.contains("AIRDROP") ) {
        	return true;
        }

        if (caption.contains("TEA PROTOCOL") ) {
        	return true;
        }
        
        if (text.contains( "AIRDROP")) {
        	return true;
        }
    	
//        // Prepare payload for ChatGPT
//        JsonObject payload = new JsonObject();
//        payload.put("model", "gpt-3.5-turbo");
//        payload.put("messages", new org.json.JSONArray()
//                .put(new JsonObject()
//                        .put("role", "system")
//                        .put("content", "Determine if the following message is spam. Respond with only 'yes' or 'no'."))
//                .put(new JsonObject()
//                        .put("role", "user")
//                        .put("content", text)));
//
//        // Send request to ChatGPT
//        String response = MyClient.postToJson( CHATGPT_API_URL, "POST", payload.toString(), CHATGPT_API_KEY);
//        JsonObject responseJson = new JsonObject(response);
//
//        if (responseJson.has("choices")) {
//            String chatGPTResponse = responseJson.getArray("choices")
//                    .getJsonObject(0)
//                    .getJsonObject("message")
//                    .getString("content");
//            return chatGPTResponse.trim().equalsIgnoreCase("yes");
//        }
        return false;
    }

    private static boolean isNumberBetween1And100(String text) {
        try {
            int number = Integer.parseInt(text.trim());
            return number >= 1 && number <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
