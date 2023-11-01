package test;

import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Object now = new Object() {
		@Override public String toString() {
			return "now()";
		}
	};
	
	static class A implements AutoCloseable {
		@Override public void close() throws Exception {
			S.out( "closing");
			throw new Exception("b");
		}
	}
	
	public static void main(String[] args) throws Exception {
		try (A a = new A() ) {
			throw new Exception("a");
		}
		catch( Exception e) {
			S.out("caught " + e);
		}
	}
	
	
}
