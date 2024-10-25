package test;

import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;



/** Just test that you can connect to the database. */
public class TestPostgres {
	static class T {
		@Override
			public String toString() {
				return "t";
			}
	}
	static record R(String name, Object t) {
	}
	
	
	public static void main(String[] args) throws Exception {
		JsonObject json = new JsonObject(); //Util.toJson( "name", new Object() );
		json.put( "name", "bob");
		json.put( "t", new Object() );
		
		R rs = json.toRecord( R.class);
		S.out( rs.toString() );
		
	}
}