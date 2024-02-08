package test;

import java.util.ArrayList;

import common.Util;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		ArrayList list = new ArrayList();
		Util.toJson( "list", list).display(); 
	}
}
