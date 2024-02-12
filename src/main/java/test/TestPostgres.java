package test;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		JsonObject obj1 = Util.toJson( "name", "bob", "age", "33");
		JsonObject obj2 = Util.toJson( "name", "sam", "age", "4");
		JsonArray ar = new JsonArray();
		ar.add( obj1);
		ar.add( obj2);
		
		JsonObject main = new JsonObject();
		main.put( "name", "hello");
		main.put( "list", ar);
		
		Util.inform( null, main.toHtml() );
	}
}
