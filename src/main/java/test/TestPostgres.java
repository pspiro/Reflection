package test;

import java.util.concurrent.Executor;

import common.Util;
import http.MyClient;
import reflection.Config;

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
	
	Executor e = new ESerial();
	
	void execute( Runnable r) {
		e.execute( r);
	}
		
		
		
	
	public static void main(String[] args) throws Exception {
		double balance = MyClient.getJson( "http://localhost:8484/hook/get-wallet-map/abc")
				.getObjectNN( "positions")
				.getDouble( "a");
	}
}
