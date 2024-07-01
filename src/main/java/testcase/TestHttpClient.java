package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ObjectHolder;
import http.ClientException;
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
		MyClient.getJson("https://reflection.trading/test502", obj -> {
			S.out( "look for the URL in the std output");
		});
		S.sleep(3000);
	}

	/** Nginx will return 502 for this endpoint */
	public static void test502() throws Exception {
		try {
			S.out( MyClient.getString("https://reflection.trading/test502") );
			assertTrue(false);
		}
		catch( ClientException e) {
			S.out(e);
			assertEquals(502, e.statusCode() );
		}
		catch( Exception e) {
			S.out(e);
			assertTrue(false);
		}
	}
	
	/** You have to kill api server for this to pass */
	public static void test404() throws Exception {
		try {
			S.out( MyClient.getString("https://reflection.trading/keke") );
			assertTrue(false);
		}
		catch( ClientException e) {
			S.out(e);
			assertEquals(404, e.statusCode() );
		}
		catch( Exception e) {
			assertTrue(false);
		}
	}
	
	/** get json object, sync */ 
	public static void testgetJson() throws Exception {
		JsonObject obj = MyClient.getJson("https://reflection.trading/api/ok");
		assertEquals("OK", obj.getString("code"));
	}

	/** get json object, async */ 
	public static void testgetJsonAsync() throws Exception {
		ObjectHolder<JsonObject> ob = new ObjectHolder<>();
		
		Util.sync( q -> {
			MyClient.getJson("https://reflection.trading/api/ok", obj -> {
				ob.val = obj;
				q.put("");
			});
		});

		assertEquals("OK", ob.val.getString("code") );
		
		Util.sync( q -> {
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
		String url = "https://reflection.trading/api/positions/" + Cookie.wallet;
		assertTrue( MyClient.getArray(url).size() > 0);
		
		ObjectHolder<JsonArray> ob = new ObjectHolder<>();
		Util.sync( q ->
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
