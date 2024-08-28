package web3;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONAware;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.web3j.crypto.Keys;

import common.Util;
import http.MyClient;
import reflection.Config;
import tw.util.MyException;
import tw.util.S;

/** id sent will be returned on response; you could batch all different query types
 *  when it gets busy */
public class NodeServer {
	public static String prod = "0x2703161D6DD37301CEd98ff717795E14427a462B";
	static String pulseRpc = "https://rpc.pulsechain.com/";

	/** you could make this a member var */
	private static String rpcUrl;  // note you can get your very own rpc url from Moralis for more bandwidth
	
	/** note that we sometimes pass rpcUrl with trailing / and sometimes not */
	public static void setChain( String rpcUrlIn) throws Exception {
		S.out( "Setting node server rpcUrl=%s", rpcUrlIn);
		rpcUrl = rpcUrlIn;
	}
	
	/** Send a query and expect "result" to contain a hex value.
	 *  A value of '0x' is invalid and will throw an exception */
	private static String queryHexResult( String body, String text, String contractAddr, String walletAddr) throws Exception {
		S.out( "Querying %s  contract='%s'  wallet='%s'", text, contractAddr, walletAddr);
		String result = nodeQuery( body).getString( "result");
		
		if (result.equals( "0x") ) {
			throw new MyException( "Could not get %s; contractAddr '%s' may be invalid", text, contractAddr);
		}

		return result;
	}

	/** This can be a node created for you on Moralis, which you pay for, or a free node.
	 *  Currently using the free node; if you hit pacing limits, switch to the Moralis node.
	 *  The Moralis node requires auth data 
	 *  
	 * note that we sometimes pass rpcUrl with trailing / and sometimes not */
	private static JsonObject nodeQuery(String body) throws Exception {
		JsonObject obj = (JsonObject)nodeQueryAll( body);
		
		JsonObject err = obj.getObject( "error");
		if (err != null) {
			throw new MyException( "nodeQuery error  code=%s  %s", err.getInt( "code"), err.getString( "message") );
		}
		return obj;
	}

	/** different nodes have different batch sizes; you can probably get bigger size
	 * with a paid node e.g. Moralis */
	static int batchSize = 10; // should be configurable
	
	private static JsonArray batchQuery( JsonArray requests) throws Exception {
		int i = 0;
		var results = new JsonArray();
		
		while (i < requests.size() ) {
			var batch = new JsonArray();
			
			for (int j = 0; j < batchSize && i < requests.size(); j++) {
				batch.add( requests.get( i++) );
			}
			
			smallBatchQuery( batch.toString() )
				.forEach( item -> results.add( item) );
		}
		
		return results;
	}

	/** query size must be <= maxBatchSize */
	private static JsonArray smallBatchQuery( String body) throws Exception {
		var anyJson = nodeQueryAll( body);
		
		if (anyJson instanceof JsonArray) {
			var ar = (JsonArray)anyJson;
			Util.require( ar.size() > 0, "ERROR: query returned no results"); // this might be okay, not sure

			if (ar.size() > 0) {
				var error = ar.get( 0).getObject( "error");
				if (error != null) {
					throw new Exception( "ERROR: batch query returned error: " + error);
				}
			}
			
			return (JsonArray)anyJson;
		}

		S.out( "ERROR: query did not return an array, it returned:");
		var obj = (JsonObject)anyJson;
		obj.display();
		JsonObject err = obj.getObjectNN( "error");  // this is just a guess, I've never seen it return this
		throw new MyException( "nodeQuery batch error  code=%s  %s", err.getInt( "code"), err.getString( "message") );
	}
	
	private static JSONAware nodeQueryAll(String body) throws Exception {
		Util.require( rpcUrl != null, "Set the Moralis rpcUrl");

		return MyClient.create( rpcUrl, body)
				.header( "accept", "application/json")
				.header( "content-type", "application/json")
				.queryToAnyJson();
	}

	/** see also getLatestBlock() */
	public static long getBlockNumber() throws Exception {
		String body = """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_blockNumber"
			}""";
		return nodeQuery( body).getLong( "result");
	}
	
	/** Get the n latest blocks and for each return the gas price that covers
	 *  pct percent of the transactions */
	public static JsonObject getFeeHistory(int blocks, int pct) throws Exception {
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_feeHistory",
			"params": [ "%s", "latest", [ %s ] ]
			}""", blocks, pct); 
		return nodeQuery( body);
	}

	/** This takes a long time and returns a lot of data */
	public static JsonObject getQueuedTrans() throws Exception {
		String body = """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "txpool_content"
				}""";
		return nodeQuery( body);  // result -> pending and result -> queued
	}

	public static double getNativeBalance(String walletAddr) throws Exception {
		String body = String.format( """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "eth_getBalance",
				"params": [ "%s", "latest" ]
				}""", walletAddr);
		
		return Erc20.fromBlockchain( nodeQuery( body).getString( "result"), 18);
	}

	public static Fees queryFees() throws Exception {
		// params are # of blocks, which percentage to look at
		JsonObject json = getFeeHistory(5, 60).getObject( "result");

		// get base fee of last/pending block
		long baseFee = Util.getLong( json.<String>getArrayOf( "baseFeePerGas").get( 0) );

		// take average of median priority fee over last 5 blocks 
		long sum = 0;
		ArrayList<ArrayList> reward = json.<ArrayList>getArrayOf( "reward");
		for ( ArrayList ar : reward) {
			sum += Util.getLong( ar.get(0).toString() );
		}

		return new Fees( baseFee * 1.2, sum / 5.);
	}
	
	public static void showTrans() throws Exception {
		Config c = Config.ask( "Dt2");
		JsonObject result = getQueuedTrans().getObject("result");

		S.out( "Pending");
		show( result.getObject( "pending"), c.ownerAddr() );
		
		S.out( "");
		S.out( "Queued");
		show( result.getObject( "queued"), c.ownerAddr() );
	}
	
	// I think the issue is that you have pending trans that will never get picked up
	// and they are blocking next trans; they have to be removed
	static void show( JsonObject obj, String addr) throws Exception {
		obj.getObjectNN( Keys.toChecksumAddress(addr) ).display();
	}

	/** see also getBlockNumber() */
	public static JsonObject getLatestBlock() throws Exception {
		// the boolean says if it gets the "full" block or not
		String body = """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_getBlockByNumber",
			"params": [	"latest", false ]
			}""";
		return nodeQuery( body);
	}
	
	/** note w/ moralis you can also get the token balance by wallet
	 * I'm assuming that "data" is the parameters encoded the same was as in Fireblocks module 
	 * @param m_address */
	public static double getTotalSupply(String contractAddr, int decimals) throws Exception {
		Util.reqValidAddress( contractAddr);
		
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_call",
			"params": [
				{
				"to": "%s",
				"data": "0x18160ddd"
				},
				"latest"
			]
			}""", contractAddr);
		
		return Erc20.fromBlockchain( queryHexResult( body, "totalSupply", contractAddr, "n/a"), decimals);
	}
	
	public static double getAllowance( String contractAddr, String approverAddr, String spenderAddr, int decimals) throws Exception {
		Util.reqValidAddress( contractAddr);
		Util.reqValidAddress( approverAddr);
		Util.reqValidAddress( spenderAddr);
		
		String body = String.format( """ 
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_call",
			"params": [
				{
				"to": "%s",
				"data": "0xdd62ed3e000000000000000000000000%s000000000000000000000000%s"
				},
				"latest"
			]
			}""", contractAddr, approverAddr.substring(2), spenderAddr.substring(2) );
		
		return Erc20.fromBlockchain( queryHexResult( body, "allowance", contractAddr, approverAddr), decimals);
	}
	
	/** map contract address (lower case) to number of Decimals, so we only query it once;
	 *  this assumes all calls are on the same blockchain */ 
	static HashMap<String,Integer> decMap = new HashMap<>();
	
	/** Return number of decimals for the contract; first time sends a query,
	 *  subsequent times are map lookup
	 *  
	 *  never called*/
	public synchronized static int getDecimals(String contractAddr) throws Exception {
		return Util.getOrCreateEx( decMap, contractAddr.toLowerCase(), () ->
			getTokenDecimals( contractAddr.toLowerCase() ) );
	}

	/** allow the user to pre-fill the decimals map to avoid sending queries when possible */
	public static void setDecimals(Erc20 token) {
		setDecimals( token.decimals(), Util.toArray(token.address() ) ); 
	}

	/** allow the user to pre-fill the decimals map to avoid sending queries when possible */
	public static void setDecimals(int decimals, String[] addresses) {
		S.out( "Pre-filling decimals map  decimals=%s  count=%s", decimals, addresses.length);
		
		for (var address : addresses) {
			decMap.put( address.toLowerCase(), decimals);
		}
	}

	private static int getTokenDecimals(String contractAddr) throws Exception {
		Util.reqValidAddress( contractAddr);

		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id":1,
			"method": "eth_call",
			"params": [
				{
				"to": "%s",
				"data":"0x313ce567"
				},
				"latest"
			]
			}
			""", contractAddr);
		
		return Erc20.decodeQuantity( queryHexResult( body, "decimals", contractAddr, "n/a")).intValue();
    }

	static class Req extends JsonObject {
		Req( String method, int id) {
			put( "jsonrpc", "2.0");
			put( "method", method);
			put( "id", id);
		}
	}
	
	static class BalReq extends Req {
		BalReq( int id, String contract, String wallet) {
			super( "eth_call", id);
			
			var param1 = Util.toJson(
					"to", contract,
					"data", "0x70a08231000000000000000000000000" + wallet.substring(2)
					);

			Object[] params = new Object[2];
			params[0] = param1;
			params[1] = "latest";
			
			put( "params", params);
		}
	}

	/** Makes a separate call for each one. If decimals is zero, it's possible that
	 *  contracts have different number of decimals.
	 *  @param decimals if zero we will look it up from the map or query for it */
	static public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		Util.reqValidAddress( walletAddr);

		// build an array of balance requests
		JsonArray ar = new JsonArray();
		for (int i = 0; i < contracts.length; i++) {
			ar.add( new BalReq( i, contracts[i], walletAddr) );
		}
		
		// submit the query
		S.out( "Querying %s positions for %s", contracts.length, walletAddr);
		var batchResult = batchQuery( ar);
		S.out( "  returned %s items", batchResult.size() );
		
		HashMap<String, Double> positionsMap = new HashMap<>();

		// build a map of contract -> position (for non-zero positions only)
		for (var single : batchResult) {
			// get index
			int index = single.getInt( "id");
			Util.require( index < contracts.length, "Index %s is out of range", index);
			
			String contractAddr = contracts[index];
			String result = single.getString( "result");
			
			if (result.equals( "0x")) {
				S.out( "Could not get balance; contractAddr '%s' may be invalid", contractAddr);
			}
			else {
				double balance = Erc20.fromBlockchain( 
						result, 
						decimals != 0 ? decimals : getDecimals( contractAddr) ); // look up or query for decimals if needed
				
				if (balance > 0) {
					positionsMap.put( contractAddr.toLowerCase(), balance);
				}
			}
		}

		return positionsMap;
	}
	
	/** get ERC-20 token balance; see also getNativeBalance();
	 *  see also getPositionMap()
	 *  @param decimals can be zero; if so, we will look it up in the map;
	 *  if not found, we will query for the value */
	public static double getBalance( String contractAddr, String walletAddr, int decimals) throws Exception {
		Util.reqValidAddress( contractAddr);
		Util.reqValidAddress( walletAddr);
		
		// query or look up number of decimals if needed
		if (decimals == 0) {
			decimals = getDecimals( contractAddr);
		}
		
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_call",
			"params": [
				{
				"to": "%s",
				"data": "0x70a08231000000000000000000000000%s"
				},
				"latest"
			]
			}""", contractAddr, walletAddr.substring( 2) );  // strip the 0x
		
		return Erc20.fromBlockchain( queryHexResult( body, "balance", contractAddr, walletAddr), decimals);
	}
	
	public static void main(String[] args) throws Exception {
		Config.ask();
		getReceipt( "0xa301f3a437a636a4a492b81ff2f23a355a1d44577ed02ff277addb1a7bcd30e5");
	}
	
	/** must handle the no receipt or not ready yet state */
	public static String getReceipt( String transHash) throws Exception {
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_getTransactionReceipt",
			"params": [
				"%s"
			]
			}""", transHash);
		
		// no result is no "result" tag, just id
		var result = nodeQuery( body);//.getObjectNN( "result").removeEntry( "logs"); // long and boring
//		boolean success = result.getString( "status").equals( "0x1");
		result.display();
		return "";
	}
	
	// to get the revert reason, make the same call, with same params, same from, to, data, but add the block number and use eth_call;
	// this simulates the call as if it were executed at the end of the specified block
//	{
//		  "jsonrpc": "2.0",
//		  "method": "eth_call",
//		  "params": [
//		    {
//		      "from": "0xYourFromAddress",
//		      "to": "0xYourContractAddress",
//		      "data": "0xYourEncodedFunctionCallData",
//		      "value": "0xYourWeiValue"
//		    },
//		    "0xYourBlockNumber"
//		  ],
//		  "id": 1
//		}

	
	
}

// notes
// you can use eth_call to get revert reason for a past block AND ALSO to see what would
// happen if you called the transaction now, on the current block
// probably you should do that before each call, then you don't need to check all
// the dif params, and 