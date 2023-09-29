package monitor;

import common.Util;
import redis.clients.jedis.Jedis;
import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.S;

public class SeedPrices {
	
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dt-config");
		Util.require( !config.isProduction(), "Not in production");
			
		Jedis jedis = config.redisPort() == 0
				? new Jedis( config.redisHost() )  // use full connection string
				: new Jedis( config.redisHost(), config.redisPort() );
	
		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		stocks.forEach( stock -> update( "" + stock.getConid(), jedis ) );
		
		S.out( "done");
	}
	
	static void update( String conid, Jedis jedis) {
		jedis.hset( conid, "bid", "100");
		jedis.hset( conid, "ask", "101");
		jedis.hset( conid, "last", "100.5");
	}
}
