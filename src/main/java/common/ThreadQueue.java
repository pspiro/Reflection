package common;

import java.util.concurrent.LinkedBlockingQueue;

import common.Util.ExRunnable;

public class ThreadQueue {
	private LinkedBlockingQueue<ExRunnable> m_queue = new LinkedBlockingQueue<>();
	
	public void start() {
		Util.execute( () -> run() );
	}
	
	public void queue( ExRunnable runnable) {
		m_queue.add( runnable);
	}
	
	private void run() {
		while (true) {
			try {
				m_queue.take().run();
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}
	}
}
