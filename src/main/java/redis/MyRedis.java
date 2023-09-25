package redis;

import java.net.URI;

import common.Util;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisURIHelper;

/** Facilitates reconnecting to Jedis if connection is lost.
 *  And we make sure that we are catching Jedis exceptions for the calls that the user makes.
 *  Handle the pipeline and set a timer to sync the pipeline.
 *  
 *  In summary, it keeps the connection open, but closes and re-opens if there is an error. */
public class MyRedis {
	private Jedis m_jedis;
	final private String m_hostOrURI;
	final private int m_port;
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
	
	/** Connect, run a single query, then disconnect */
	public <T> T singleQuery( Jrt<T> runnable) {
		T t = query(runnable);
		disconnect();
		return t;
	}
	
	/** Run a single query and leave the connection open */
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
	}


	/** Use this version when all the queries are done at once. It sends the real query
	 *  after all the little queries have been added to the pipeline */
	public void pipeline( PRun runnable) {
		wrap( () -> {
			checkConnection();
			Pipeline pipeline = m_jedis.pipelined();
			runnable.run(pipeline);
			pipeline.sync();
		});
	}

	private void wrap( Runnable run) {
		try {
			run.run();
		}
		catch( JedisException e) {
			m_jedis = null;
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
