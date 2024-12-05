package test;

import org.json.simple.JsonObject;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		String json = """
				{
"approved": 0,
"wallet": "0x2703161d6dd37301ced98ff717795e14427a462e",
"native": 0,
"positions": {}
}
				""";
		JsonObject.parse( json).getObject( "positions").getString( "abc");
	}
}
