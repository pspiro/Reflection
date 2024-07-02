package test;

import org.json.simple.JsonObject;
import org.json.simple.TsonArray;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static class A extends JsonObject {
	}
	
	static class MyList extends TsonArray<A> {
	}
	
	public static void main(String[] args) throws Exception {
	}
}
