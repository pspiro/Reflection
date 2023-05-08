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
		values.put( "last", "24");
		jedis.xadd( "ibm", StreamEntryID.NEW_ENTRY, values);

		getLast( "ibm");
	}
	private static void getLast(String stream) {
		List<StreamEntry> item = jedis.xrevrange(stream, "+", "-", 1); // strange, it gives all values
		S.out(item);
		
	}
}

/*
 * xadd <name> <id> <key> <val> <key> <val> ...
 * use * for id
 * 
 *  xlen <name> returns length (# of entries) of stream
 *  
 * xrange <name> <from> <to> count <n>
 * xrevrange
 * use - and + for first and beyond last
 * xrevrange + -
 * 
 * xread count <n> streams <name> <name> ... <start-id> <start-id>  ...
 * returns the next entry after start-id
 * use id 0 for all, e.g. stream ibm ge 0 0
 * 
 * xread block <n> streams <name> 
 * use id $ for last to always block
 * 
 * 
*/