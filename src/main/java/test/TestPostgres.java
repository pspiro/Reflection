package test;

import java.util.Timer;
import java.util.TimerTask;

import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		TimerTask task1 = new TimerTask() {
			@Override public void run() {
				S.out( "start 1");
				S.sleep( 2000);
				S.out( "  end 1");
			}
		};

		TimerTask task2 = new TimerTask() {
			@Override public void run() {
				S.out( "start 2");
				S.sleep( 2000);
				S.out( "  end 2");
			}
		};
		
		Timer timer = new Timer();
		timer.schedule( task1, 0);
		timer.schedule( task2, 0);
	}
}
