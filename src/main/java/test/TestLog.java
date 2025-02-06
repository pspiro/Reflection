package test;

import org.json.simple.JsonObject;

import common.LogType;
import common.MyTimer;
import testcase.MyTestCase;

/** compare speed of using a single db connection vs creating a new connection each time */
public class TestLog extends MyTestCase {
	
	public void test() throws Exception {
		JsonObject data = new JsonObject();
		data.put("enum", LogType.REC_ORDER);
		data.put("test", "T1");
		
		JsonObject json = new JsonObject();
		json.put("wallet_public_key", "my wallet");
		json.put("type", LogType.AUTO_FILL);
		json.put("uid", "my id");
		json.put("data", data);
		
		MyTimer t = new MyTimer();
		t.next("a");
		for (int i = 0; i < 50; i++) {  // avg 185 ms per 
			m_config.sqlCommand( conn -> {
				conn.insertJson("log", json);
			});
		}
		
		data.put("test", "T2");
		t.next("b");
		m_config.sqlCommand( conn -> {  // avg 22 ms per, almost ten times faster
			for (int i = 0; i < 50; i++) {
				conn.insertJson("log", json);
			}
		});
		t.done();
	}
}
