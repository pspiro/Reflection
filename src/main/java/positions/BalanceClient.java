package positions;

import java.sql.ResultSet;
import java.util.HashMap;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;

import tw.util.S;

/** This is a client that will query Moralis for the token balances.
 *  We should compare the speed to the streaming approach. */

public class BalanceClient {
	public static void main(String[] args) {
		try {
			new BalanceClient().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void run() throws Exception {
		MoralisServer.m_database.connect( "jdbc:postgresql://localhost:5432/reflection", "postgres", "1359");
		
		ResultSet res = MoralisServer.m_database.query( "select distinct wallet from events");
		while (res.next() ) {
			String wallet = res.getString(1);
		}
		
		HashMap<String, Stock> map = EventFetcher.readStocks();
		for (Stock stock : map.values() ) {
			query( stock.token() );
			break;
		}
	}
	
	void query( String token) {
		
		String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		String chain = EventFetcher.chain;
		String tokens = String.format( "&token_address=%s", token);
		
	    String url = String.format( "https://deep-index.moralis.io/api/v2/%s/erc20?chain=%s%s", wallet, chain, tokens);
	    
	// correct: https://deep-index.moralis.io/api/v2/0xb016711702D3302ceF6cEb62419abBeF5c44450e/erc20?chain=goerli&token_addresses=0x97F0D430Ed153986D9f3Fa8C5Cff9b45c3e6a9Ad' \

	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		
		S.out( "  querying Moralis " + url);
		
		client.prepare("GET", url)
		  	.setHeader("accept", "application/json")
		  	.setHeader("X-API-Key", "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6")
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			S.out( "received " + obj.getResponseBody() );
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	});  // add .join() here to make it syncronous
	    
	}
	
}
