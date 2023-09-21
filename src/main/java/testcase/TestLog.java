package testcase;

import org.json.simple.JsonObject;
import org.postgresql.util.PGobject;

import util.LogType;

public class TestLog extends MyTestCase {
	
	public void test() throws Exception {
		JsonObject data = new JsonObject();
		data.put("name", "peter");
		data.put("enum", LogType.REC_ORDER);
		
		JsonObject json = new JsonObject();
		json.put("wallet_public_key", "my wallet");
		json.put("type", LogType.AUTO_FILL);
		json.put("uid", "my id");
		json.put("data", data);
		
		m_config.sqlCommand( conn -> {
			conn.insertJson("log", json);
		});
		
		JsonObject obj = m_config.sqlQuery( conn -> conn.queryToJson("select * from log where created_at = (select max(created_at) from log)") ).get(0);
		assertEquals( "my wallet", obj.get("wallet_public_key") );
		assertEquals( LogType.AUTO_FILL.toString(), obj.get( "type") );
		assertEquals( "my id", obj.get("uid"));
		
		JsonObject readData = JsonObject.parse( ((PGobject)obj.get("data")).getValue() );
		assertEquals( LogType.REC_ORDER.toString(), readData.getString("enum") );
	}
}
