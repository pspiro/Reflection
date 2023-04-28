package testcase;

import junit.framework.TestCase;
import redis.MyRedis;
import redis.MyRedis.Prt;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import reflection.Config;
import tw.util.S;

public class TestMyRedis extends TestCase {
	static Config config;
	
	static {
		try {
			config = Config.readFrom("Dt-config");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void testSetAndGet() {
		MyRedis redis = config.newRedis();
		redis.run( jedis -> jedis.set( "a", "b") );
		assertEquals( "b", redis.query( jedis -> jedis.get( "a") ) );
	}
	
	public void testBreak1() {
		MyRedis redis = config.newRedis();
		
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
		MyRedis redis = config.newRedis();
		
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
		MyRedis redis = config.newRedis();

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
}
