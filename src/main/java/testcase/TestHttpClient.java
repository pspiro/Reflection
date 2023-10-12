package testcase;

import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

import org.json.simple.JsonObject;

import common.Util.ObjectHolder;
import http.MyClient;

public class TestHttpClient extends MyTestCase {
	/** get string, async 
	 * @throws Exception */
	public static void testgetString() throws Exception {
		String str = MyClient.getString("https://reflection.trading/api/ok");
		assertTrue( str.contains("OK") );
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

	/** get json array, async */
	public static void testgetArray() {
		assertTrue(false);
	}

	/** post to json object, async */
	public static void testpostToJson() {
	}
}
