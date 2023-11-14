package testcase;

import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ObjectHolder;
import http.MyClient;
import tw.util.S;

public class TestHttpClient extends MyTestCase {
	/** get string, async 
	 * @throws Exception */
	public static void testgetString() throws Exception {
		String str = MyClient.getString("https://reflection.trading/api/ok");
		assertTrue( str.contains("OK") );
	}

	/** Nginx will return 502 for this endpoint */
	public static void testExc() throws Exception {
		MyClient.getJson("https://reflection.trading/api/ok", obj -> {
			S.out( "look for the URL in the std output");
			throw new Exception("Aack!");
		});
		S.sleep(3000);
	}

	/** Nginx will return 502 for this endpoint */
	public static void test502() throws Exception {
		try {
			S.out( MyClient.getString("https://reflection.trading/test502") );
			assertTrue(false);
		}
		catch( Exception e) {
			assertTrue(true);
		}
	}
	
	/** You have to kill api server for this to pass */
	public static void test404() throws Exception {
		try {
			S.out( MyClient.getString("https://reflection.trading/keke") );
			assertTrue(false);
		}
		catch( Exception e) {
			assertTrue(true);
		}
	}
	
	/** get json object, sync */ 
	public static void testgetJson() throws Exception {
		JsonObject obj = MyClient.getJson("https://reflection.trading/api/ok");
		assertEquals("OK", obj.getString("code"));
	}

	/** This performs the action and wait for it to complete;
	 *  The action must signal completion by calling q.put(""); 
	 * @throws InterruptedException */
	public static void sync( Consumer<SynchronousQueue<String>> consumer) throws InterruptedException {
		SynchronousQueue<String> q = new SynchronousQueue<>();
		consumer.accept(q);
		q.take();
	}
		
		
	/** get json object, async */ 
	public static void testgetJsonAsync() throws Exception {
		ObjectHolder<JsonObject> ob = new ObjectHolder<>();
		
		sync( q -> {
			MyClient.getJson("https://reflection.trading/api/ok", obj -> {
				ob.val = obj;
				q.put("");
			});
		});

		assertEquals("OK", ob.val.getString("code") );
		
		sync( q -> {
			MyClient.postToJson("https://reflection.trading/api/ok", "abc", obj -> {
				ob.val = obj;
				q.put("");
			});
		});
		
		assertEquals("OK", ob.val.getString("code") );
	}

	/** get json array, async 
	 * @throws Exception */
	public static void testgetArray() throws Exception {
		String url = "https://reflection.trading/api/positions/0x76274e9a0f0bc4eb9389e013bd00b2c4303cdd37";
		assertTrue( MyClient.getArray(url).size() > 0);
		
		ObjectHolder<JsonArray> ob = new ObjectHolder<>();
		sync( q ->
			MyClient.getArray(url, ar -> {
				ob.val = ar;
				q.put("");
			}));
		assertTrue( ob.val.size() > 0);
	}

	/** post to json object, async */
	public static void testpostToJson() {
	}
}
