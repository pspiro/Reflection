package web3;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util.ExFunction;
import web3.NodeInstance.Transfer;

public class NodeAux {
	private static final String RPC_URL = "YOUR_NODE_RPC_URL";  // Replace with your node's RPC URL
	private static final String JSON_RPC_VERSION = "2.0";
	private static final String METHOD = "eth_getLogs";  // To fetch transaction logs (for ERC-20 transfers)
	static String pad = "0x000000000000000000000000";
	
	static JsonObject createReq( String[] addresses, int fromBlock, String from, String to) {
		// Set up filtering by contract addresses
		var topics = new ArrayList<String>();
		topics.add( NodeInstance.transferEventSignature);
		topics.add( pad( from) );
		topics.add( pad( to) );

		// Step 1: Build the JSON request body
		JsonObject params = new JsonObject();
		params.put("fromBlock", "" + fromBlock);  // You can use "latest" or specific block numbers
		params.put("toBlock", "latest");
		params.put("topics", topics);
		params.put("address", addresses);  // Only for the given contract addresses

		JsonArray plist = new JsonArray();
		plist.add( params);

		// Create JSON-RPC request body
		JsonObject requestBody = new JsonObject();
		requestBody.put("jsonrpc", JSON_RPC_VERSION);
		requestBody.put("method", METHOD);
		requestBody.put("id", 1);
		requestBody.put("params", plist);

		return requestBody;
	}

	/** NOTE address, sender, and recipient will all be lower case
	 *  @map function might send a query to get the decimals */
	static List<Transfer> processResult( JsonObject json, ExFunction<String,Integer> map) throws Exception {
		List<Transfer> transactions = new ArrayList<>();
		
		JsonArray logs = json.getArray("result");
		for (var log : logs) {
			String address = log.getLowerString("address");
			Transfer transfer = parseLog( log, map.apply( address) );
			if (transfer != null) {
				transactions.add( transfer); 
			}
		}
		
		return transactions;
	}
	
	static String pad(String wallet) {
		return wallet == null ? null : pad + wallet.substring( 2);
	}

	static public Transfer parseLog( JsonObject log, int decimals) throws Exception {
		ArrayList<String> topics = log.<String>getArrayOf("topics");
		
		if (topics.size() >= 3) {
			return new Transfer( 
					log.getLowerString("address"),
					"0x" + topics.get(1).substring(26).toLowerCase(), 
					"0x" + topics.get(2).substring(26).toLowerCase(), 
					Erc20.fromBlockchain( log.getString("data"), decimals),
					log.getLong( "blockNumber"),
					log.getString( "transactionHash")
					);
		}
		return null;
	}
}
