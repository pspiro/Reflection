package test;

import chain.ChainParams;
import common.Util;
import tw.util.S;


/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		var json = Util.toJson( "a", 3, "chainId", 1);
		
		var rec = json.toRecord(ChainParams.class);
		S.out( rec);
	}
}
