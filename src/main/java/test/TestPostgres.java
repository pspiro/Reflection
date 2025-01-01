package test;

import org.json.simple.JsonObject;

import common.Util;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		String json = """
				{"id": 71567649,"message":{"date":1734463923,"reply_to_message":{"date":1734460269,"chat":{"id":-1001262398926,"title":"Reflection Community","type":"supergroup","username":"ReflectionTrading"},"message_id":36222,"from":{"id":6009241172,"is_bot":false,"first_name":"Голубь","username":"Arsenka908"},"story":{"chat":{"last_name":"SPIN","id":7673947479,"type":"private","first_name":"TON"},"id":4}},"chat":{"id":-1001262398926,"title":"Reflection Community","type":"supergroup","username":"ReflectionTrading"},"message_thread_id":36222,"message_id":36228,"from":{"language_code":"en","is_premium":true,"last_name":"Spiro | Reflection","id":5053437013,"is_bot":false,"first_name":"Pete","username":"peterspiro"},"text":"OK"}}
				""";
		var obj = JsonObject.parse( json);
		obj.getObject( "message").update( "date", date -> Util.yToS.format( date) );
		obj.display();
	}
}
