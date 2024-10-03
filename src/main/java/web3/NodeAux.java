package web3;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util.ExFunction;
import reflection.Config;
import tw.util.S;
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

	/** NOTE address, sender, and recipient will all be lower case */
	static List<Transfer> processResult( JsonObject json, String wallet, ExFunction<String,Integer> map) throws Exception {
		List<Transfer> transactions = new ArrayList<>();
		
		wallet = wallet.toLowerCase(); // Convert wallet address to lower case to ensure matching is case-insensitive


		// you have to pad the topics so they are 32 bytes
		// 0x + 000000000000000000000000 + walletWithout0x
		
		JsonArray logs = json.getArray("result");

		// Step 5: Filter transactions for contract transfers involving the wallet
		for (var log : logs) {
			String address = log.getLowerString("address");
			ArrayList<String> topics = log.<String>getArrayOf("topics");
			
			// Ensure the log has at least two topics: event signature and sender/recipient
			if (topics.size() >= 3) {  // Ensure there are at least 3 topics (event signature, sender, recipient)
			    String sender = "0x" + topics.get(1).substring(26).toLowerCase();  // Extract sender address
			    String recipient = "0x" + topics.get(2).substring(26).toLowerCase();  // Extract recipient address
				double val = Erc20.fromBlockchain( log.getString("data"), map.apply( address) );
				
				// Only consider transfers where the wallet is either sender or recipient
				if ( (sender.equals(wallet) || recipient.equals(wallet)) && val > 0) {

					transactions.add( new Transfer( 
							address, 
							sender, 
							recipient, 
							val,
							log.getLong( "blockNumber"),
							log.getString( "transactionHash")
							) );
				}
			}
		}
		
		return transactions;
	}
	
	static String pad(String wallet) {
		return wallet == null ? null : pad + wallet.substring( 2);
	}
	
}
