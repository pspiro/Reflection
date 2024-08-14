package test;

import java.util.concurrent.Executor;

import common.Util;
import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config c;

	static {
		try {
			//c = Config.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	static class ESerial implements Executor {
		@Override public void execute(Runnable run) {
			Util.executeEvery(0, 0, run);
		}
	}

	static class ENow implements Executor {
		@Override public void execute(Runnable run) {
			run.run();
		}
	}
	
	public static void main(String[] args) throws Exception {
		Executor e = new ESerial();
		
		for (int i = 0; i < 3; i++) {
			Util.execute( () -> e.execute( () -> S.out( "a")) ); 
		}
	}
}
