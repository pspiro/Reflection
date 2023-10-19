package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExRunnable;
import fireblocks.Accounts;
import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Config;
import tw.util.S;

public class MyTestCase extends TestCase {
	public static String dead = "0x000000000000000000000000000000000000dead";

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
		return postOrder(obj).readJsonObject();
	}

	String postOrderToId( JsonObject obj) throws Exception {
		return postOrder(obj).readJsonObject().getString("id");
	}

	JsonObject getWorkingLiveOrder(String id) throws Exception {
		JsonArray msgs = getAllLiveOrders(Cookie.wallet).getArray("orders");
		return msgs != null ? msgs.find( "id", id) : null;
	}

	public JsonObject getAllLiveOrders(String address) throws Exception {
		return cli().get("/api/working-orders/" + address)
				.readJsonObject();
	}
	
	JsonObject getLiveMessage2(String id) throws Exception {
		JsonArray msgs = getCompletedLiveOrders();
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
	JsonObject getLiveMessage(String id) throws Exception {
		// wait a tic for the order to fill, even autoFill orders take a few ms
		S.sleep(1000);
		
		JsonArray msgs = getCompletedLiveOrders();
		msgs.display();
		for (JsonObject msg : msgs) {
			if (msg.getString("id").equals(id) ) {
				return msg;
			}
		}
		throw new Exception("No live order found with id " + id);
	}
	
	JsonArray getCompletedLiveOrders() throws Exception {
		return getAllLiveOrders(Cookie.wallet).getArray("messages");
	}

	protected void assert200() throws Exception {
		if (cli.getResponseCode() != 200) {
			S.out( "%s - %s", cli.getRefCode(), cli.getMessage() );
		}
		assertEquals( 200, cli.getResponseCode() );
	}
	
	protected void assertNotEquals(String notExpected, String actual) {
		assertTrue( 
				String.format( "Got %s which was not expected", notExpected),
				!notExpected.equals(actual) );
	}
	
	/** Modify a config setting, refresh RefAPI, run some code, restore the setting */
	void modifySetting(String key, String val, ExRunnable run) throws Exception {
		String saved = m_config.getSetting(key);
		try {
			m_config.setSetting(key, val);
			cli().get("/api/?msg=refreshConfig").readString();
			assert200();
			S.out( "Modified config setting '%s' to '%s'", key, val);
			
			run.run();
		}
		finally {
			m_config.setSetting(key, saved);
			cli().get("/api/?msg=refreshConfig").readString();
			assert200();
			S.out( "Restored config setting '%s' to '%s'", key, saved);
		}
	}
	
}
