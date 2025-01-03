package web3;

import java.util.ArrayList;

import org.json.simple.JsonObject;

import common.Util.ExFunction;
import web3.NodeInstance.Transfer;
import web3.NodeInstance.Transfers;

public class NodeAux {
	private static final String RPC_URL = "YOUR_NODE_RPC_URL";  // Replace with your node's RPC URL
	private static final String JSON_RPC_VERSION = "2.0";
	private static final String METHOD = "eth_getLogs";  // To fetch transaction logs (for ERC-20 transfers)
	static String pad = "0x000000000000000000000000";
	
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
					log.getString( "transactionHash"),
					"" // timestamp
					);
		}
		return null;
	}
}
