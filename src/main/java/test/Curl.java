package test;

import org.json.simple.JsonObject;

import http.MyClient;
import tw.util.S;

public class Curl {
	/** Usage: curl <url> [tag,val] [tag,val] ... */
	public static void main(String[] args) throws Exception {
		String url = args[0];
		
		JsonObject json = new JsonObject();
		
		for (int i = 1; i < args.length; i++) {
			json.put( args[i++], args[i]);
		}
		
		S.out( MyClient.create( url, json.toString() )
			.queryToAnyJson().toJSONString() );
	}
}
