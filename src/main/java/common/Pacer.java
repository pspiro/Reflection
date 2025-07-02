package common;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class Pacer implements Closeable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final Runnable m_runnable;
    private ScheduledFuture<?> task; // tracks the scheduled task; this can also get the result of the executions, if there were one; the ? represents the return type
	private final long m_ms;

	public Pacer(long ms, Runnable runnable) {
		m_ms = ms;
		m_runnable = runnable;
	}

	/** NOTE: it's possible that a call comes in while we are executing; in that case,
	 *  it would not restart the call; to fix, you would need to synchronize the execution
	 *  of m_runnable */
	public synchronized void start() {
        if (task == null || task.isDone()) {
            task = scheduler.schedule(m_runnable, m_ms, TimeUnit.MILLISECONDS);
        }
	}

	/** Finish last task, if necessary, and terminate the thread so the program may close */
	@Override
	public synchronized void close() {
		// wait for remaining tasks
		if (task != null) { // need to check if task is done?
			Util.wrap( () -> task.get() );
		}

		// close thread so program can terminate; does not wait for tasks to execute
		scheduler.shutdown();
	}
}
