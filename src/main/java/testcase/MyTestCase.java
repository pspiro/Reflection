package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.Util;

public class MyTestCase extends TestCase {
	static Config m_config;
	
	protected MyHttpClient cli;  // could probably just change this to static and remove client()	
	
	static {
		try {
			m_config = Config.readFrom("Dt-config");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	MyHttpClient cli() throws Exception {
		return (cli=new MyHttpClient("localhost", 8383));
	}

	public static void startsWith( String expected, String got) {
		assertEquals( expected, Util.left( got, expected.length() ) );
	}
	
	MyHttpClient postOrder( MyJsonObject obj) throws Exception {
		return cli().post( "/api/reflection-api/order", obj.toString() ); 
	}

	MyJsonObject postOrderToObj( MyJsonObject obj) throws Exception {
		return postOrder(obj).readMyJsonObject();
	}
	
}
