package test;

import java.util.Timer;
import java.util.TimerTask;

import tw.util.S;

/** This class is for chaining together processes that are asyncronous.
 *  You could add a parameter for processes to pass a value to the next in the chain.
 *  You could add a catch method for exceptions.
 */
public class Promise {
	private Promise m_parent;
	private Promise m_nextPromise;
	private PromTask m_handler; 
	boolean m_completed;

	interface PromTask {
		void run( Promise p);
	}

	public Promise() {
	}

	public Promise(Promise parent) {
		m_parent = parent;
	}

	static Promise fetchData(Promise promise) {

		S.out( "fetching data start");
		later( 100, () -> {
			S.out( "fetching data end");
			promise.onToNext();
		});

		return promise;
	}

	static Promise processData(Promise promise) {
		S.out( "processing data start");
		later( 100, () -> {
			S.out( "processing data end");
			promise.onToNext();
		});
		return promise;
	}

	static void require(boolean v) {
		if (!v) {
			throw new RuntimeException();
		}
	}
	
	public Promise then(PromTask handler) {
		require(!m_completed);
		require(m_handler == null);
		
		m_handler = handler;
		
		assert m_nextPromise == null;
		return m_nextPromise = new Promise(this);
	}
	
	public void onToNext() {
		require(!m_completed);
		
		m_completed = true;
		
		if (m_handler != null) {
			m_handler.run( m_nextPromise);
		}
	}
	
	static PromTask success2 = new PromTask() {
		@Override public void run(Promise p) {
			S.out( "success 2a");

			new Timer().schedule( new TimerTask() {
				@Override public void run() {
					S.out( "success 2b");
					p.onToNext();
				}
			}, 0);
		}
		
	};

	public static void later(long ms, Runnable runnable) {
		Timer t = new Timer();
		t.schedule( new TimerTask() {
			@Override public void run() {
				t.cancel();
				runnable.run();
			}
		}, ms);
	}
	
	public static void main(String[] args) {
		// this works BUT ONLY IF the fetchData() returns before it processes the data;
		// only the first one in the chain has this limitation
		fetchData( new Promise() )
			.then( prom -> processData( prom) )
			.then( prom -> fetchData( prom) )
			.then( prom -> S.out( "done") );
		
		// this works all the time
		new Promise()
			.then( Promise::fetchData)
			.then( prom -> fetchData( prom) )
			.then( Promise::processData)
			.then( prom -> S.out( "done") )
			.start();

	}

	private void start() {
		if (m_parent != null) {
			m_parent.start();
		}
		else {
			onToNext();
		}
	}
}


// I think this is all wrong and that the initial function call does all the queries
// and returns a set number of chained promises
