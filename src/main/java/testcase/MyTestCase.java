package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Accounts;
import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Config;
import tw.util.S;

public class MyTestCase extends TestCase {
	static Config m_config;
	static Accounts accounts = Accounts.instance;
	
	protected MyHttpClient cli;  // could probably just change this to static and remove client()	
	
	static {
		try {
			m_config = Config.readFrom("Dt-config");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	MyHttpClient cli() throws Exception {
		return (cli=new MyHttpClient("localhost", 8383));
	}

	public static void startsWith( String expected, String got) {
		assertEquals( expected, Util.left( got, expected.length() ) );
	}
	
	MyHttpClient postOrder( JsonObject obj) throws Exception {
		return cli().post( "/api/order", obj.toString() ); 
	}

	JsonObject postOrderToObj( JsonObject obj) throws Exception {
		return postOrder(obj).readMyJsonObject();
	}

	String postOrderToId( JsonObject obj) throws Exception {
		return postOrder(obj).readMyJsonObject().getString("id");
	}

	JsonObject getLiveOrders(String address) throws Exception {
		return cli().get("/api/working-orders/" + address)
				.readMyJsonObject();
	}
	
	JsonArray getLiveMessages() throws Exception {
		return getLiveOrders(Cookie.wallet).getArray("messages");
	}
	
	JsonObject getLiveMessage(String id) throws Exception {
		// wait a tic for the order to filled, even autoFill orders take a few ms
		S.sleep(100);
		
		JsonArray msgs = getLiveMessages();
		msgs.display();
		for (JsonObject msg : msgs) {
			if (msg.getString("id").equals(id) ) {
				return msg;
			}
		}
		throw new Exception("No live order found with id " + id);
	}
	
	JsonObject getLiveMessage2(String id) throws Exception {
		JsonArray msgs = getLiveMessages();
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
	JsonObject getLiveOrder(String id) throws Exception {
		JsonArray msgs = getLiveOrders(Cookie.wallet).getArray("orders");
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
}
