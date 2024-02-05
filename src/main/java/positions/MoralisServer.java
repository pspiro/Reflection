package positions;


import java.net.http.HttpResponse;
import java.util.function.Consumer;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import http.MyClient;
import tw.util.S;

/** This app keeps the positions of all wallets in memory for fast access.
 *  This is not really useful because the queries from Moralis are really quick */
public class MoralisServer {
	public static String chain;  // or eth
	static final String moralis = "https://deep-index.moralis.io/api/v2.2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final String transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	
	public static String queryBalances(String contract) throws Exception {
		String url = String.format( "%s/%s/erc20/balances?chain=%s", moralis, contract, chain);
		return querySync( url);
	}
	
	public static JsonObject queryTransaction( String transactionHash) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		String url = String.format( "%s/transaction/%s?chain=%s",
				moralis, transactionHash, chain);
		return JsonObject.parse( querySync( url) );
	}

	/** Send the query; if there is an UnknownHostException, try again as it
	 *  may resolve the second time */ 
	public static String querySync(String url) throws Exception {
		return MyClient.create(url)
				.header("accept", "application/json")
				.header("X-API-Key", apiKey)
				.query().body();
	}

	public static String contractCall( String contractAddress, String functionName, String abi) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		String url = String.format( "%s/%s/function?chain=%s&function_name=%s",
				moralis, contractAddress, chain, functionName);
		return post( url, abi);
	}

	public static String post(String url, String body) throws Exception {
		HttpResponse<String> resp = MyClient.create(url, body)
				.header("accept", "application/json")
				.header("content-type", "application/json")
				.header("X-API-Key", apiKey)
				.query(); 
		Util.require( resp.statusCode() == 200,
				"Moralis error  url=%s  code=%s  body=%s",
				url, resp.statusCode(), resp.body() );
		return resp.body();
	}
	
	/** Fields returned:
 		symbol : BUSD,
		balance : 4722366482869645213697,
		possible_spam : true,
		decimals : 18,
		name : Reflection BUSD,
		token_address : 0x833c8c086885f01bf009046279ac745cec864b7d
		@param addresses is the list of token addresses for which we want the positions */
	public static JsonArray reqPositionsList(String wallet, String[] addresses) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		
		String url = String.format("%s/%s/erc20?chain=%s", moralis, wallet, chain);
		
		for (int i = 0; i < addresses.length; i++) {
			url += String.format("&token_addresses[%s]=%s", i, addresses[i]);
		}
		
		String ret = querySync(url);
		
		// we expect an array; if we get an object, there must have been an error
		if (JsonObject.isObject(ret) ) {
			throw new Exception( "Moralis " + JsonObject.parse(ret).getString("message") );
		}

		return JsonArray.parse( ret);
	}
	
	/** For ERC-20 token, tells you how much the spender is authorized to spend on behalf of owner.
	 *  In our case, token is non-RUSD stablecoin, owner is the user, and spender is RUSD */  
	public static JsonObject reqAllowance(String contract, String owner, String spender) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		String url = String.format("%s/erc20/%s/allowance?chain=%s&owner_address=%s&spender_address=%s",
				moralis, contract, chain, owner, spender);
		return JsonObject.parse( querySync(url) );
	}
	
	public static double getNativeBalance(String address) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		String url = String.format("%s/%s/balance?chain=%s", moralis, address, chain);
		return Erc20.fromBlockchain(
				JsonObject.parse( querySync(url) ).getString("balance"),
				18);
	}

	/** Seems useless; returns e.g.
	 * {"nfts":"0","collections":"0","transactions":{"total":"0"},"nft_transfers":{"total":"0"},"token_transfers":{"total":"0"}} */
	public static String getWalletStats(String wallet) throws Exception {
		String url = String.format( "%s/wallets/%s/stats?chain=%s", moralis, wallet, chain);
		return querySync( url);
		
	}

	/** useless e.g. {"transfers":{"total":"0"}} */
	public static String getErc20Stats(String address) throws Exception {
		String url = String.format( "%s/erc20/%s/stats?chain=%s", moralis, address, chain);
		return querySync( url);
	}
	
	/** this works for transfer events, which probably catches everything, but not
	 *  my custom events such as BuyRusd and SellRusd, which don't even appear in
	 *  the logs on PolyScan
	 * @param address
	 * @param topic
	 * @return
	 * @throws Exception
	 */
	public static String logs(String address, String topic) throws Exception {
		String url = String.format( "%s/%s/logs?chain=%s&topic0=%s", moralis, address, chain, topic);
		return querySync(url);
		
	}
	
	/** returns one page of transactions for a specific token
	 *  @address is ERC20 token address */
	public static JsonObject getTokenTransfers(String address, String cursor) throws Exception {
		String url = String.format( "%s/erc20/%s/transfers?chain=%s&cursor=%s", moralis, address, chain, S.notNull(cursor) );
		return JsonObject.parse( querySync(url) );
	}
	
	/** returns one page of transactions for a specific token
	 *  @address is ERC20 token address */
	public static JsonObject getWalletTransfers(String address, String cursor) throws Exception {
		String url = String.format( "%s/%s/erc20/transfers/?chain=%s&cursor=%s", moralis, address, chain, S.notNull(cursor) );
		return JsonObject.parse( querySync(url) );
	}
	
	interface Query {
		JsonObject produce(String cursor) throws Exception;
	}
	
	/** Query for all the data, one page at a time, and call consumer.accept() with each page 
	 *  @producer is the method that queries for one page of data
	 *  @consumer is the method that processes one page of data */
	public static void getAll(Consumer<JsonArray> consumer, Query producer) throws Exception {
		String cursor = "";
		
		while (true) {
			JsonObject full = producer.produce(cursor);
			
			consumer.accept( full.getArray("result") );
			
			cursor = full.getString( "cursor");
			if (S.isNull(cursor) ) {
				break;
			}
			S.sleep(10);  // don't break pacing limits
		}
	}	
	
	/** returns all transactions for a specific token */
	public static void getAllTokenTransfers(String address, Consumer<JsonArray> consumer) throws Exception {
		getAll( consumer, cursor -> getTokenTransfers(address, cursor) );  
	}
	
	/** returns all transactions for a specific token */
	public static void getAllWalletTransfers(String address, Consumer<JsonArray> consumer) throws Exception {
		getAll( consumer, cursor -> getWalletTransfers(address, cursor) );  
	}
	
	public static void main(String[] args) throws Exception {
//		Config.ask();
//		getAllTokenTransfers("0x4470033bd3cbf4f4f6ac4076b1085f819c7d0844", ar -> ar.display() );
		//getAllWalletTransfers("0xa14749d89e1ad2a4de15ca4463cd903842ffc15d", ar -> ar.display() );
		
//		String str = logs(
//				"0x4d5bacafecbd57e28098b5f1be7a40df96f0fa2c",
//				//"0xf4e116c5af669bd0b672b4498a0a9b172a0029e608d2ab109e51480f6abc8414"
//				transferTopic
//				);
//		JsonObject.parse(str).display();
	}
	
}

// topic0 is full keccak of the event (initial cap)

//transferTopic
//0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
