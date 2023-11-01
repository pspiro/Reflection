package test;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Object now = new Object() {
		@Override public String toString() {
			return "now()";
		}
	};

	public static void main(String[] args) throws Exception {
		Config.readFrom("Dt-config").sendEmail("peteraspiro@gmail.com", "abc", "def");		
	}
	
	
}
