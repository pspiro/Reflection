package redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;
import reflection.Util;

/** Facilitates reconnecting to Jedis if connection is lost.
 *  And we make sure that we are catching Jedis exceptions for the calls that the user makes.  */
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
		try {
			checkConnection();
			runnable.run(m_jedis);
		}
		catch( JedisException e) {
			m_jedis = null;
			throw e;
		}
	}
	
	public <T> T query( Jrt<T> runnable) {
		try {
			checkConnection();
			return runnable.run(m_jedis);
		}
		catch( JedisException e) {
			m_jedis = null;
			throw e;
		}
	}

	/** Use this version when all the queries are done at once. */
	public void fullPipeline( PRun runnable) {
		try {
			checkConnection();
			Pipeline pipeline = m_jedis.pipelined();
			runnable.run(pipeline);
			pipeline.sync();
		}
		catch( JedisException e) {
			m_jedis = null;
			throw e;
		}
	}

//	class P {
//		Pipeline m_pipeline;
//		
//		P( Pipeline pipeline) {
//			m_pipeline = pipeline;
//		}
//		
//		public void run( JRun runnable) throws Exception {
//			try {
//				Util.require(m_pipeline != null, "Not in pipeline for pipe");
//				Util.require(m_jedis != null, "Not in pipeline for pipe");
//				runnable.run(m_jedis);
//			}
//			catch( JedisException e) {
//				m_jedis = null;
//				throw e;
//			}
//		}
//		
//		public <T> T query( Jrt<T> runnable) {
//			try {
//				checkConnection();
//				return runnable.run(m_jedis);
//			}
//			catch( JedisException e) {
//				m_jedis = null;
//				throw e;
//			}
//		}
//		
//		
//	}
	
	/** This has severe limitation of only one pipeline at a time.
	 *  Use this version when all the queries are spread out over time */
	public void pipelined() {
		try {
			checkConnection();
			m_pipeline = m_jedis.pipelined();
		}
		catch( JedisException e) {
			m_jedis = null;
			throw e;
		}	
	}
	
	public void pipeRun( PRun runnable) throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");
		
		try {
			runnable.run(m_pipeline);
		}
		catch( JedisException e) {
			m_jedis = null;
			m_pipeline = null;
			throw e;
		}
	}
	
	public void pipeQuery( Prt<T> runnable) throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");
		
		try {
			return runnable.run(m_pipeline);
		}
		catch( JedisException e) {
			m_jedis = null;
			m_pipeline = null;
			throw e;
		}
	}
	
	public void sync(Pipeline pipeline) throws Exception {
		Util.require( m_jedis != null, "No connection");
		Util.require( m_pipeline != null, "Not in pipeline");
		
		try {
			m_pipeline.sync();
		}
		catch( JedisException e) {
			m_jedis = null;
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
		public void run(Jedis runnable);
	}

	public interface Jrt<T> {
		public T run(Jedis runnable);
	}

	public interface PRun {
		public void run(Pipeline runnable);
	}
	
	public interface Prt<T> {
		public T run(Pipeline runnable);  // you can change all to take JedisCommands
	}
}
