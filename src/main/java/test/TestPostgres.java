package test;

import org.json.simple.JsonArray;

import common.Util;
import tw.util.S;



/** Just test that you can connect to the database. */
public class TestPostgres {
	static record A( String name) {}
	
	public static void main(String[] args) throws Exception {
//		Config c = Config.ask("pulse");
//		Config.setSingleChain();
		JsonArray l = new JsonArray();
		l.add( Util.toJson( "name", "bob") );
		
		var list = l.toRecord( A.class);
		S.out( list);
		
	}
	
	
}