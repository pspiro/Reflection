package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;

public class TestSql extends MyTestCase {
	public void test() throws Exception {
		String key = "lksjdf" + Util.rnd.nextInt(1000);
		
		JsonObject obj = new JsonObject();
		obj.put( "wallet_public_key", key);
		obj.put( "first_name", "test");
		obj.put( "last_name", null);

		// insert it
		m_config.sqlCommand( sql -> sql.insertJson( "users", obj) );
		
		JsonArray ar = m_config.sqlQuery( "select * from users where wallet_public_key = '%s'", key);
		ar.display();
		assertEquals( 1, ar.size() );
		assertEquals( null, ar.get(0).get("last_name") );
		
		// delete it
		m_config.sqlCommand( sql -> sql.delete("delete from users where wallet_public_key = '%s'", key) );

		ar = m_config.sqlQuery( "select * from users where wallet_public_key = '%s'", key);
		assertEquals( 0, ar.size() );
	}
}
