package redis;

import java.util.ArrayList;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import tw.util.S;

public class TestRedis {
	static Jedis jedis = new Jedis("34.125.38.193", 3001);

	static Integer[] conids = { 8314, 756733, 320227571 };
	
	static class PriceQuery {
		private int conid;
		private Response<Map<String, String>> res;

		public PriceQuery(Pipeline p, int conid) {
			this.conid = conid;
			res = p.hgetAll("" + conid);
		}
	}

	interface JRun {
		public void run(Pipeline p);
	}
	
	static void jquery(JRun run) {
		Pipeline p = jedis.pipelined();
		run.run(p);
		p.sync();
	}
	
	public static void main(String[] args) throws Exception {
		
		ArrayList<PriceQuery> list = new ArrayList<PriceQuery>(); 

		jquery( pipeline -> {
			for (Integer conid : conids) {
				list.add( new PriceQuery(pipeline, conid) );
			}
		});

		for (PriceQuery q : list) {
			Map<String, String> prices = q.res.get();
			S.out( "%s bid=%s ask=%s last=%s close=%s", 
					q.conid, prices.get("bid"), prices.get("ask"), prices.get("last"), prices.get("close") );
		}
	}
	
	void getVals() {
		Pipeline p = jedis.pipelined();
		Response<String> bid = p.get( "8314:bid");
		Response<String> ask = p.get( "8314:ask");
		Response<String> lst = p.get( "8314:last");
		Response<String> cls = p.get( "8314:close");
		p.sync();
		
		S.out( "bid=%s ask=%s last=%s close=%s",
				bid.get(), ask.get(), lst.get(), cls.get() );
		
//		Map<String, String> all = jedis.hgetAll("8314");
//		for (Entry<String, String> iter : all.entrySet()  ) {
//			S.out( "%s=%s", iter.getKey(), iter.getValue() );
//		}
	}
	
	static void sendMulti() {
		Pipeline p = jedis.pipelined();
		for (int i = 0; i < 100; i++) {
			S.out( jedis.get( "abc:def:" + i) );
		}
		
	}

	static void sendTrans() {
	
		Transaction t = jedis.multi();
		for (int i = 0; i < 100; i++) {
			S.out( "setting " + i);
			t.set( "abc:def:" + i, "" + (i+1));  // each one takes 80 ms
		}
		t.exec();
		
		S.out( jedis.get( "abc:def:4") );
	
	}
}


