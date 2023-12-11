package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.S;

public class TestPartialFill extends MyTestCase {
	
	private static double curPrice = TestOrder.curPrice;

	// fill order buy order
	public void testMore() throws Exception {
		JsonObject obj = TestOrder.createOrder( "BUY", 10, 3);
		obj.put("simPartial", 5);
		
		String uid = postOrderToId(obj);
		assert200();
		
		S.sleep(1000);

		confirmLog( "select * from log where uid = '%s' and type = 'PARTIAL_FILL", uid );
		confirmLog( "select * from log where uid = '%s' and type = 'ORDER_COMPLETED", uid );
		
		JsonObject t = m_config.sqlQuery( query -> query.queryToJson("select * from transactions where uid = '%s'", uid ) ).get(0);
		assertEquals(5, t.getInt("quantity") );
		assertEquals(5, t.getInt("roundedQuantity") );
		assertEquals(m_config.commission() / 2, t.getDouble("commission") );
		//assertEquals(5, t.getDouble("tds"));
	}

	public void testLess() throws Exception {
		JsonObject obj = TestOrder.createOrder( "BUY", 10, 3);
		obj.put("simPartial", 4);
		
		String uid = postOrderToId(obj);
		assert200();
		
		S.sleep(1000);
		confirmLog( "select * from log where uid = '%s' and type = 'PARTIAL_FILL", uid);
		confirmLog( "select * from log where uid = '%s' and type = 'ORDER_FAILED", uid);
	}
	
	void confirmLog(String str, Object... args) throws Exception {
		JsonArray ar = m_config.sqlQuery( query -> query.queryToJson(str, args) );
		assertTrue(ar.size() == 1);
	}
}
