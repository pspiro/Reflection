package common;

import java.util.Timer;
import java.util.TimerTask;

/** All calls to a single instance this execute in the same thread */
public class NiceTimer {
	private final Timer m_timer;
	private boolean m_scheduled;

	public NiceTimer(String name) {
		m_timer = new Timer( name);
	}

	/** This can be called multiple times but will only allow one to be scheduled.
	 *  NOTE you can only call this with the same Runnable each time;
	 *  it could be improved to support different schedules for different Runnables*/
	public synchronized void schedule( int wait, Runnable runnable) {
		if (!m_scheduled) {
			
			m_scheduled = true;

			m_timer.schedule( new TimerTask() {
				@Override public void run() {
					synchronized( NiceTimer.this) {
						m_scheduled = false;
					}
					runnable.run();
				};
			}, wait);
		}
	}

	/** Just adds the convenience of not having to create a TimerTask */
	public void execute( Runnable runnable) {
		executeEvery( 0, 0, runnable);
	}

	/** Just adds the convenience of not having to create a TimerTask */
	public void executeEvery(int wait, int period, Runnable runnable) {
		TimerTask task = new TimerTask() {
			@Override public void run() {
				runnable.run();
			}
		};

		if (period > 0) {
			m_timer.schedule( task, wait, period);
		}
		else {
			m_timer.schedule( task, wait);
		}
	}
}
