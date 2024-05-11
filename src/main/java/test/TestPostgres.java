package test;

import java.util.HashSet;

import org.json.simple.JsonArray;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static HashSet<String> s = new HashSet<String>();
	static JsonArray ar = new JsonArray();
	
	
	public static void main(String[] args) throws Exception {
		process( Config.ask( "Dev"));
		process( Config.ask( "Prod"));
	}


	private static void process(Config config) throws Exception {
		config.sqlQuery( "select * from signup").forEach( rec -> {
			String email = rec.getString("email").toLowerCase();
			if (!s.contains( email) ) {
				s.add( email);
				ar.add( rec);
			}
		});
		
		ar.print();
	}
}
