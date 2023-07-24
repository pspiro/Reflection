package redis;

import java.net.URI;

import common.Util;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;

/** Facilitates reconnecting to Jedis if connection is lost.
 *  And we make sure that we are catching Jedis exceptions for the calls that the user makes.
 *  Handle the pipeline and set a timer to sync the pipeline  */
public class MyRedis {
	private Jedis m_jedis;
	final private String m_hostOrURI;
	final private int m_port;
	private Pipeline m_pipeline;
	private String m_name;
	
	/** You should only use this if you are leaving the connection open */
	public void setName( String name) {
		m_name = name;
		if (m_jedis != null) {
			m_jedis.clientSetname(name);
		}
	}
	
	public MyRedis(String uri) throws Exception {
		this( uri, 0);
	}
	
	public MyRedis(String hostOrURI, int port) throws Exception {
		m_hostOrURI = hostOrURI;
		m_port = port;
		
		if (m_port == 0) {
			Util.require( JedisURIHelper.isValid(URI.create(hostOrURI)), "redis connect string is invalid" );
		}
	}
	
	/** NOTE: you should have your try/catch outside the scope of the run() call
	 *  so Jedis exceptions are caught here */
	public void run( JRun runnable) {
		wrap( () -> {
			checkConnection();
			runnable.run(m_jedis);
		});
	}
	
	public <T> T singleQuery( Jrt<T> runnable) {
		T t = query(runnable);
		disconnect();
		return t;
	}
	
	public <T> T query( Jrt<T> runnable) {
		try {
			checkConnection();
			return runnable.run(m_jedis);
		}
		catch( JedisException e) {
			disconnect();
			throw e;
		}
	}

	/** Do NOT throw an exception on disconnect */
	public void disconnect() {
		if (m_jedis != null) {
			try {
				m_jedis.disconnect();
			}
			catch( Exception e) { } // swallow it
		}
		m_jedis = null;
		m_pipeline = null;
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

	/** Currently not used.
	 * 
	 *  This has severe limitation of only one pipeline at a time.
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
	
	/** Currently not used; 
	 * 
	 *  goes along with startPipeline() */
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
				? new Jedis(m_hostOrURI)
				: new Jedis(m_hostOrURI, m_port);
			if (m_name != null) {
				m_jedis.clientSetname(m_name);
			}
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
