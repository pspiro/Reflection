package test;

import java.util.HashMap;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntry;
import tw.util.S;

public class RedisServer {
	static Jedis jedis = new Jedis("redis://default:IfwvoOjg8LakUsunTBf1zTIh8rPMmyzQ@redis-13467.c302.asia-northeast1-1.gce.cloud.redislabs.com:13467");
	public static void main(String[] args) {
		
		HashMap<String,String> values = new HashMap<>();
		values.put( "bid", "22");
		values.put( "ask", "23");
		jedis.xadd( "ibm", StreamEntryID.NEW_ENTRY, values);

		// it somehow is putting a map, how does it do that?, as if each item i
		values.put( "bid", "24");
		values.put( "pop", "25");
		jedis.xadd( "ibm", StreamEntryID.NEW_ENTRY, values);
		
		getLast( "ibm");
	}
	private static void getLast(String stream) {
		List<StreamEntry> item = jedis.xrevrange(stream, "+", "-", 1); // strange, it gives all values
		S.out(item);
		
	}
}
