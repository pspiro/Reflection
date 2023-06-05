package testcase;

import fireblocks.Accounts;
import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.Util;

public class MyTestCase extends TestCase {
	static Config m_config;
	static Accounts accounts = Accounts.instance;
	
	protected MyHttpClient cli;  // could probably just change this to static and remove client()	
	
	static {
		try {
			m_config = Config.readFrom("Dt-config");
			accounts.setAdmins( "Admin1,Admin2");
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
	
	MyHttpClient postOrder( MyJsonObject obj) throws Exception {
		return cli().post( "/api/order", obj.toString() ); 
	}

	MyJsonObject postOrderToObj( MyJsonObject obj) throws Exception {
		return postOrder(obj).readMyJsonObject();
	}

	String postOrderToId( MyJsonObject obj) throws Exception {
		return postOrder(obj).readMyJsonObject().getString("id");
	}

	MyJsonObject getLiveOrders(String address) throws Exception {
		return cli().get("/api/working-orders/" + address)
				.readMyJsonObject();
	}
	
	MyJsonArray getLiveMessages() throws Exception {
		return getLiveOrders(Cookie.wallet).getAr("messages");
	}
	
	MyJsonObject getLiveMessage(String id) throws Exception {
		MyJsonArray msgs = getLiveMessages();
		for (MyJsonObject msg : msgs) {
			if (msg.getString("id").equals(id) ) {
				return msg;
			}
		}
		throw new Exception("No live order found with id " + id);
	}
	
	MyJsonObject getLiveMessage2(String id) throws Exception {
		MyJsonArray msgs = getLiveMessages();
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
	MyJsonObject getLiveOrder(String id) throws Exception {
		MyJsonArray msgs = getLiveOrders(Cookie.wallet).getAr("orders");
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
}
