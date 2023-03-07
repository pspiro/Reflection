package test;

import junit.framework.TestCase;
import redis.MyRedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisException;
import tw.util.S;

public class TestRedisSpeed extends TestCase {
	static String addr = "34.125.60.132";
	
	public void testQuery() {
		MyRedis redis = new MyRedis(addr, 6379);
		redis.run( jedis -> jedis.set( "a", "b") );
		assertEquals( "b", redis.query( jedis -> jedis.get( "a") ) );
	}
	
	static Response<String> obj1;
	static Response<String> obj2;

	public void testPipeline() {
		MyRedis redis = new MyRedis(addr, 6379);
		redis.pipeline( pipeline -> {
			pipeline.set( "c", "d");
			pipeline.set( "e", "f");
		});
		
		redis.pipeline( pipeline -> {
			obj1 = pipeline.get("c");
			obj2 = pipeline.get("e");
		});
		
		assertEquals( "d", obj1.get() );
		assertEquals( "f", obj2.get() );
	}

	public void testTwoPipes() {
		Jedis redis = new Jedis(addr, 6379);
		Pipeline p1 = redis.pipelined();
		Pipeline p2 = redis.pipelined();
		
		p1.set("a", "b");
		p2.set("a", "c");
		
		p2.sync();
		p1.sync();
		
		assertEquals( "b", redis.get( "a") );
	}
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();

		final MyRedis redis = new MyRedis(addr, 6379);
		redis.run( jedis -> jedis.connect() );

		S.out( "setting");
		for (int i = 0; i < 5; i++) {
			try {
				redis.run( jedis -> jedis.set( "a", "b");
			}
			catch( JedisException e) {
				S.out( e.getMessage() );
			}
			S.sleep(1000);
		}

		S.out( "getting");
		for (int i = 0; i < 5; i++) {
			try {
				S.out( redis.query( jedis -> jedis.get( "a") ) );
			}
			catch( JedisException e) {
				S.out( e.getMessage() );
			}
			S.sleep(1000);
		}

		S.out( "setting pipeline full cycle");
		for (int i = 0; i < 5; i++) {
			try {
				redis.pipeline( pipeline -> {
					pipeline.jedis.set( "a", "b");
					pipeline.jedis.set( "c", "d");
				});
			}
			catch( JedisException e) {
				S.out( e.getMessage() );
			}
			S.sleep(1000);
		}

		S.out( "setting pipeline broken out");

		for (int i = 0; i < 5; i++) {
			try {
				redis.run( jedis -> jedis.set( "a", "b");
			}
			catch( JedisException e) {
				S.out( e.getMessage() );
			}
			S.sleep(1000);
		}
	}

}


//		for (int i = 0; i < 20; i++) {
//			try {
//				redis.pipeline( pipeline -> {
//					obj = pipeline.get("a");
//				});
//				S.out( obj.get() );
//				S.sleep(1000);
//			}
//			catch( JedisException e) {
//				e.printStackTrace();
//			}
//		}
//
//		S.out( System.currentTimeMillis() - start);
//	}
//}
