package http;

import java.util.Map;

import reflection.MySqlConnection;
import reflection.ParamMap;
import tw.util.S;

public class TestConnection {
	public static void main(String[] args) {
		Map<String, String> env = System.getenv();
		S.out( env);
		
		SimpleTransaction.listen("0.0.0.0", 8383, trans -> {
			try {
				handle_(trans);
			} catch (Exception e) {
				trans.respond( "handle error: " + e.getMessage() );
			}
		});
	}

	private static void handle_(SimpleTransaction trans) throws Exception {
		S.out( "----------");
		trans.showAll();
		
		ParamMap map = trans.getMap();
		String msg = map.get("msg").intern();
		
		switch(msg) {
			case "testdb":
				testdb( trans, map);
				break;
				
			case "testip":
				testip( trans, map);
				break;

			default:
				trans.respond( "supported msg types: testdb (url,user,pw), testip (host,port,op,cmd)");
		}
	}

	private static void testdb(SimpleTransaction trans, ParamMap map) throws Exception {
		String url = map.get("url");
		String user = map.get("user");
		String pw = map.get("pw");
		
		MySqlConnection conn = new MySqlConnection();
		conn.connect( url, user, pw);

		trans.respond( "Connected");
	}

	private static void testip(SimpleTransaction trans, ParamMap map) throws Exception {
		String host = map.get("host");
		int port = map.getRequiredInt("port");
		String op = map.get("op");
		String cmd = map.get("cmd");

		MyHttpClient client = new MyHttpClient(host, port);
		if (op.equals( "GET") ) {
			client.get( cmd);
		}
		else {
			client.post( cmd);
		}
		trans.respond( client.readString() );
	}
}
