package test;

import redis.MyRedis;
import tw.util.S;

public class TestRedis {
	public static void main(String[] args) throws Exception {
		
		MyRedis redis = new MyRedis("redis://default:IfwvoOjg8LakUsunTBf1zTIh8rPMmyzQ@redis-13467.c302.asia-northeast1-1.gce.cloud.redislabs.com:13467");
		redis.connect();
		redis.setName("bob");
		S.out( "check name bob");
		S.sleep(5000);
		redis.disconnect();
		redis.setName("sam");
		redis.connect();
		S.out( "check name sam");
		S.sleep(5000);
	}
}
