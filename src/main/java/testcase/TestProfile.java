package testcase;

import org.json.simple.JsonObject;

import reflection.Config;

public class TestProfile extends MyTestCase {
	public void testSetProfile() throws Exception {
		String json = """
{
"name": "john glick",
"address": "addresss",
"email": "emaill",
"phone": "8383838383",
"pan_number": "pann",
"wallet_public_key": "0xb016711702D3302ceF6cEb62419abBeF5c44450g"
}
		""";
		
		Config c = Config.readFrom("Dt-config");
		c.sqlCommand( conn -> { 
			conn.execute("delete from users where name = 'john glick'");
		});
		
		cli().post("/api/update-profile", json);
		cli.assertResponseCode(200);
		
		cli().get("/api/get-profile/0xb016711702D3302ceF6cEb62419abBeF5c44450g");
		JsonObject obj = cli.readJsonObject();
		assertEquals( "john", obj.getString("first_name") );
		assertEquals( "glick", obj.getString("last_name") );
		assertEquals( "addresss", obj.getString("address") );
		assertEquals( "emaill", obj.getString("email") );
		assertEquals( "8383838383", obj.getString("phone") );
		assertEquals( "pann", obj.getString("pan_number") );
		
		String json2 = """
{
"first_name": "timmy",
"last_name": "jones",
"address": "hot",
"email": "cold",
"phone": "open",
"pan_number": "closed",
"wallet_public_key": "0xb016711702D3302ceF6cEb62419abBeF5c44450g"
}
		""";
		
		cli().post("/api/update-profile", json2);
		cli.assertResponseCode(200);
		
		cli().get("/api/get-profile/0xb016711702D3302ceF6cEb62419abBeF5c44450g");
		obj = cli.readJsonObject();
		assertEquals( "timmy", obj.getString("first_name") );
		assertEquals( "jones", obj.getString("last_name") );
		assertEquals( "hot", obj.getString("address") );
		assertEquals( "cold", obj.getString("email") );
		assertEquals( "open", obj.getString("phone") );
		assertEquals( "closed", obj.getString("pan_number") );
		
	}
}
