package test;

import common.Util;
import http.MyClient;
import reflection.Config;
import tw.util.S;



/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
//		Config c = Config.ask("pulse");
//		Config.setSingleChain();
	
		var json = Util.toJson( 
				"code", "lwjkefdj827",
				"wwwname", "lkjsdf");
		
		String pwurl = "https://pwserver-6idtjuv2oq-el.a.run.app/getpw";
		MyClient.postToJson( pwurl + "/getpw", json.toString() ).display();
	}
	
	
}