package test;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.SingleChainConfig;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		String json = """
[
{ "time": 1733433909980, "action": "hello" },
{ "time": 1733433909980, "action": "mamma" }
]
				""";
		
		JsonObject obj = Util.toJson( 
				"first", "peterrr", 
				"actions", JsonArray.parse( json) );
		obj.display();
		
		var actions = obj.getArray( "actions");
		actions.update( "time", val -> Util.yToS.format( val) );
		obj.display();
	}
}