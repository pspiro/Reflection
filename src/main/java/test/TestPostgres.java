package test;

import reflection.Config;
import tw.util.S;
import web3.NodeInstance;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		var config = Config.ask("dev3");
	}
}
