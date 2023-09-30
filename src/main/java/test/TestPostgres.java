package test;

import java.util.concurrent.LinkedBlockingQueue;

import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Object now = new Object() {
		@Override public String toString() {
			return "now()";
		}
	};

	public static void main(String[] args) throws Exception {
		//Config config = Config.readFrom("Dt-config");
		LinkedBlockingQueue q = new LinkedBlockingQueue();
		
		
		q.remove();
		S.out("done");
		
	}
}
