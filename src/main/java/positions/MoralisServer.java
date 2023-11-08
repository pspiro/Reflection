package positions;


import java.net.http.HttpResponse;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import fireblocks.Erc20;
import http.MyClient;
import reflection.Config;
import reflection.MySqlConnection;

/** This app keeps the positions of all wallets in memory for fast access.
 *  This is not really useful because the queries from Moralis are really quick */
public class MoralisServer {
	public static String chain;  // or eth
	static final String moralis = "https://deep-index.moralis.io/api/v2.2";
	static final String apiKey = "2R22sWjGOcHf2AvLPq71lg8UNuRbcF8gJuEX7TpEiv2YZMXAw4QL12rDRZGC9Be6";
	static final String transferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	
	enum Status { building, waiting, rebuilding, ready, error };

	// this is so fucking weird. it works when run from command prompt, but
	// when run from eclipse you can't connect from browser using external ip 
	// http://69.119.189.87  but you can use 192.168.1.11; from dos prompt you
	// can use either. pas
	static MySqlConnection m_database = new MySqlConnection();

	
	// TODO
	// set a single null at the end if you don't want to re-read everything again at startup
	// you can query by date; that would be better, then you only need to know the start date
	// see if you can set up the database to make them all lower case.
	// test that you can handle events while you are sending out the client requests 
	// double-check the synchronization
	// you should periodically query for the current balance and compare to what you have to check for mistakes
	
	public static void main(String[] args) throws Exception {
		//JsonObject t = queryTransaction("0xda3de0d726fdea7eb60af8afc3921e981f48b26e1d08daf5846aee8e3706973d", "polygon");
		//t.display();
		Config config = Config.ask();
		config.busd().queryTotalSupply();
	}
	
	public static JsonObject queryTransaction( String transactionHash, String chain) throws Exception {
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
		token_address : 0x833c8c086885f01bf009046279ac745cec864b7d */
	public static JsonArray reqPositionsList(String wallet) throws Exception {
		Util.require(chain != null, "Set the Moralis chain");
		String url = String.format("%s/%s/erc20?chain=%s",
				moralis, wallet, chain);
		String ret = querySync(url);
		
		// we expect an array; if we get an object, there must have been an error
		if (JsonObject.isObject(ret) ) {
			throw new Exception( "Moralis " + JsonObject.parse(ret).getString("message") );
		}

		return JsonArray.parse( ret);
	}
	
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
		String url = String.format( "%s/wallets/%s/stats", moralis, wallet);
		return querySync( url);
		
	}

	/** useless e.g. {"transfers":{"total":"0"}} */
	public static String getErc20Stats(String address) throws Exception {
		String url = String.format( "%s/erc20/%s/stats", moralis, address);
		return querySync( url);
	}
	
}
