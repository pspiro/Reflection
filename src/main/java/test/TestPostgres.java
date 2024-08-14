package test;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	
	static Config c;

	static {
		try {
			//c = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
	}
}
