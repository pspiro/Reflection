package redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import reflection.Util;

/** Facilitates reconnecting to Jedis if connection is lost.
 *  And we make sure that we are catching Jedis exceptions for the calls that the user makes.
 *  Handle the pipeline and set a timer to sync the pipeline  */
public class MyRedis {
	private Jedis m_jedis;
	final private String m_host;
	final private int m_port;
	private Pipeline m_pipeline;
	
	public MyRedis(String host, int port) {
		m_host = host;
		m_port = port;
	}
	
	/** We use a connection string with password, etc */
	public MyRedis(String host) {
		m_host = host;
		m_port = 0;
	}
	
	/** NOTE: you should have your try/catch outside the scope of the run() call
	 *  so Jedis exceptions are caught here */
	public void run( JRun runnable) {
		wrap( () -> {
			checkConnection();
			runnable.run(m_jedis);
		});
	}
	
	public <T> T query( Jrt<T> runnable) {
		try {
			checkConnection();
			return runnable.run(m_jedis);
		}
		catch( JedisException e) {
			m_jedis = null;
			m_pipeline = null;
			throw e;
		}
	}

	/** Use this version when all the queries are done at once. */
	public void pipeline( PRun runnable) {
		wrap( () -> {
			checkConnection();
			Pipeline pipeline = m_jedis.pipelined();
			runnable.run(pipeline);
			pipeline.sync();
		});
	}

	/** This has severe limitation of only one pipeline at a time.
	 *  Use this version when all the queries are spread out over time
	 *  @param batchTimeMs is time until we call sync
	 *  @param handler will handle exceptions during sync */
	public synchronized void startPipeline(int batchTimeMs, ExHandler handler) {
			if (m_pipeline == null) {
				wrap( () -> {
					checkConnection();
					m_pipeline = m_jedis.pipelined();
					Util.executeIn( batchTimeMs, () -> {
						try {
							sync();
						}
						catch( Exception e) {
							handler.handle(e);
						}
					});
				});
			}
	}
	
	public void runOnPipeline( PRun runnable) throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");
		
		wrap( () -> {
			runnable.run(m_pipeline);
		});
	}

	/** Not used. */
	public <T> T queryOnPipeline( Prt<T> runnable) throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");

		// wrap doesn't work here
		try {
			return runnable.run(m_pipeline);
		}
		catch( JedisException e) {
			m_jedis = null;
			m_pipeline = null;
			throw e;
		}
	}
	
	private synchronized void sync() throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");
		
		wrap( () -> {
			m_pipeline.sync();
			m_pipeline = null;
		});
	}
	
	
	private void wrap( Runnable run) {
		try {
			run.run();
		}
		catch( JedisException e) {
			m_jedis = null;
			m_pipeline = null;
			throw e;
		}
	}
	
	private synchronized void checkConnection() {
		if (m_jedis == null) {
			m_jedis = m_port == 0
				? new Jedis(m_host)
				: new Jedis(m_host, m_port);
		}
	}
	
	public interface JRun {
		void run(Jedis runnable);
	}

	public interface Jrt<T> {
		T run(Jedis runnable);
	}

	public interface PRun {
		void run(Pipeline runnable);
	}
	
	public interface Prt<T> {
		T run(Pipeline runnable);  // you can change all to take JedisCommands
	}
	
	public interface ExHandler {
		void handle( Exception e);
	}

	/** This is not needed but it's called at startup so we will fail if we can't connect */
	public void connect() {
		checkConnection();
		m_jedis.connect();
	}
}
