package testcase;

import org.json.simple.JsonObject;

import reflection.RefCode;
import tw.util.S;

public class TestSplitDates extends MyTestCase {
//	13751
//	13824
//	13977
//	3206042
	
	private JsonObject createOrder(String side, double qty, double offset, int conid) throws Exception {
		double price = m_config.newRedis().singleQuery( 
				jedis -> Double.valueOf( jedis.hget("" + conid, "bid") ) );

		JsonObject obj = TestOrder.createOrder2(side, qty, price * 1.05);
		obj.put( "conid", conid);
		return obj;
	}

	
	public void testOk1() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13751);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "OK1 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testPreSplit() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13824);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "Pre split " + text);
		assertEquals( RefCode.PRE_SPLIT.toString(), ret);
	}

	public void testOk2() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13977);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "OK2 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testPostSplit() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 3206042);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "Post split " + text);
		assertEquals( RefCode.POST_SPLIT.toString(), ret);
	}

}
