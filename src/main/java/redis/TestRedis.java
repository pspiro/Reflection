package redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class TestRedis {

	
	public static void main(String[] args) throws Exception {
		Jedis jedis = new Jedis("34.125.124.211", 6379);
		
		Pipeline pipeline = jedis.pipelined();
		
		String conid = "756733";
		pipeline.hset( conid, "bid", "23");
		pipeline.hset( conid, "ask", "24");
		
		pipeline.sync();
	}
	
}

