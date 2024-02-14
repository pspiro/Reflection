package test;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		JsonObject obj1 = new JsonObject();
		obj1.put( "name", null);
		obj1.display();
		S.out( "%s", obj1.get("name") );
	}
}
