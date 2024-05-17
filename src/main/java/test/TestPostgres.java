package test;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		Config.ask("Dt").rusd().addOrRemoveAdmin(
				"Owner",
				"0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3", 
				true).displayHash();
	}
}
