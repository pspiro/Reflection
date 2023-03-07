package test;

import junit.framework.TestCase;
import redis.MyRedis;
import redis.MyRedis.Prt;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import tw.util.S;

public class TestMyRedis extends TestCase {
	static String addr = "34.125.38.193";
	

	/** This test should fail if we connect and succeed if not */
	public void testConnect() {
		try {
			MyRedis redis = new MyRedis(addr, 6379);
			redis.connect();
			assertTrue(false);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testSetAndGet() {
		MyRedis redis = new MyRedis(addr, 6379);
		redis.run( jedis -> jedis.set( "a", "b") );
		assertEquals( "b", redis.query( jedis -> jedis.get( "a") ) );
	}
	
	public void testBreak1() {
		MyRedis redis = new MyRedis(addr, 6379);
		
		//Util.executeIn(2500, () -> redis.run( jedis -> jedis.disconnect()));
		
		for (int i = 0; i < 3; i++) {
			try {
				// test it both ways, get and set
				// S.out( redis.query( jedis -> jedis.get( "a") ) );
//				S.out( "setting");
//				redis.run( jedis -> jedis.set( "a", "b") );
				S.out( "test pipeline1");
				redis.pipeline( pipeline -> {
					pipeline.set( "c", "d");
					pipeline.set( "e", "f");
				});
				S.sleep(1000);
			}
			catch( Exception e) {
				S.out( e.getMessage() );
			}
		}
	}
	
	Response<String> obj1;
	Response<String> obj2;

	public void testPipeline1() {
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

	public void testPipeline2() throws Exception {
		MyRedis redis = new MyRedis(addr, 6379);

		redis.startPipeline(500, ex -> ex.printStackTrace() );
		redis.runOnPipeline( pipeline -> pipeline.set( "m1", "m2") );
		redis.startPipeline(500, ex -> ex.printStackTrace() );
		redis.runOnPipeline( pipeline -> pipeline.set( "m3", "m4") );
		// set break in here
		S.out( "waiting to sync");
		S.sleep(3000);

		assertEquals( "m2", redis.query( jedis -> jedis.get("m1") ) );
		assertEquals( "m4", redis.query( jedis -> jedis.get("m3") ) );
	}
//	
//	public static void main(String[] args) {
//		long start = System.currentTimeMillis();
//
//		final MyRedis redis = new MyRedis(addr, 6379);
//		redis.run( jedis -> jedis.connect() );
//
//		S.out( "setting");
//		for (int i = 0; i < 5; i++) {
//			try {
//				redis.run( jedis -> jedis.set( "a", "b") );
//			}
//			catch( JedisException e) {
//				S.out( e.getMessage() );
//			}
//			S.sleep(1000);
//		}
//
//		S.out( "getting");
//		for (int i = 0; i < 5; i++) {
//			try {
//				S.out( redis.query( jedis -> jedis.get( "a") ) );
//			}
//			catch( JedisException e) {
//				S.out( e.getMessage() );
//			}
//			S.sleep(1000);
//		}
//
//		S.out( "setting pipeline full cycle");
//		for (int i = 0; i < 5; i++) {
//			try {
//				redis.pipeline( pipeline -> {
//					pipeline.set( "a", "b");
//					pipeline.set( "c", "d");
//				});
//			}
//			catch( JedisException e) {
//				S.out( e.getMessage() );
//			}
//			S.sleep(1000);
//		}
//
//		S.out( "setting pipeline broken out");
//
//		for (int i = 0; i < 5; i++) {
//			try {
//				redis.run( jedis -> jedis.set( "a", "b") );
//			}
//			catch( JedisException e) {
//				S.out( e.getMessage() );
//			}
//			S.sleep(1000);
//		}
//	}
//
//}
//
//
////		for (int i = 0; i < 20; i++) {
////			try {
////				redis.pipeline( pipeline -> {
////					obj = pipeline.get("a");
////				});
////				S.out( obj.get() );
////				S.sleep(1000);
////			}
////			catch( JedisException e) {
////				e.printStackTrace();
////			}
////		}
////
////		S.out( System.currentTimeMillis() - start);
////	}
}
