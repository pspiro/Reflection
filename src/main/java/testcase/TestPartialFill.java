package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import tw.util.S;

public class TestPartialFill extends MyTestCase {
	static {
		try {
			TestOrder.createValidUser();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	// IMPORTANT TEST because it tests that transactions are added to the database properly
	public void testMore() throws Exception {
		JsonObject obj = TestOrder.createOrderWithOffset( "BUY", 10, 3);
		obj.put("simPartial", 5);
		
		String uid = postOrderToId(obj);
		S.out( "Test more uid " + uid);
		assert200();
		
		S.sleep(1000);

		confirmLog( "select * from log where uid = '%s' and type = 'PARTIAL_FILL'", uid );
		confirmLog( "select * from log where uid = '%s' and type = 'ORDER_COMPLETED'", uid );  // fails because we don't wait long enough
		
		JsonObject t = m_config.sqlQuery( query -> query.queryToJson("select * from transactions where uid = '%s'", uid ) ).get(0);
		t.display();
		assertEquals(5, (int)t.getDouble("quantity") );
		assertEquals(m_config.commission() / 2, t.getDouble("commission") );
		// you should check that the blockchain amounts are correct as well
		//assertEquals(5, t.getDouble("tds"));
	}
	// test rounding up and down
	
	/** This order fails because it is below the 10% threshold 
	 * 
	 *  This test fails because we no longer have a minimum threshold */
	public void testLess() throws Exception {
		JsonObject obj = TestOrder.createOrderWithOffset( "BUY", 11, 3);
		obj.put("simPartial", 1);
		
		String uid = postOrderToId(obj);
		assert200();
		
		S.sleep(1000);
		confirmLog( "select * from log where uid = '%s' and type = 'PARTIAL_FILL'", uid);
		confirmLog( "select * from log where uid = '%s' and type = 'ORDER_FAILED'", uid);
	}
	
	void confirmLog(String str, Object... args) throws Exception {
		JsonArray ar = m_config.sqlQuery( query -> query.queryToJson(str, args) );
		assertTrue(ar.size() == 1);
	}
	
	/** Start with a valid profile */
	public void testCreateValidProfile() throws Exception {
		m_config.sqlCommand( sql -> sql.insertOrUpdate(
				"users", 
				TestProfile.createProfileNC(),
				"wallet_public_key = '%s'",
				Cookie.wallet.toLowerCase() ) );
	}		
}
