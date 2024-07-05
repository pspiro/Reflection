package common;

import java.util.Timer;
import java.util.TimerTask;

/** All calls to a single instance this execute in the same thread */
public class NiceTimer {
	private final Timer m_timer = new Timer();

	public void executeEvery(int wait, int period, Runnable runnable) {
		TimerTask task = new TimerTask() {
			@Override public void run() {
				runnable.run();
			}
		};

		if (period >= 0) {
			m_timer.schedule( task, wait, period);
		}
		else {
			m_timer.schedule( task, wait);
		}
	}
}
