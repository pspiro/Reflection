package web3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONAware;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import common.Util;
import fireblocks.FbRusd;
import http.MyClient;
import reflection.Config;
import tw.util.MyException;
import tw.util.S;
import web3.Param.Address;
import web3.Param.BigInt;
import web3.RetVal.NewRetVal;

/** id sent will be returned on response; you could batch all different query types
 *  when it gets busy */
public class NodeInstance {
    static final String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	public static String prod = "0x2703161D6DD37301CEd98ff717795E14427a462B".toLowerCase();
	public static final String nullAddr = "0x0000000000000000000000000000000000000000";
	public static final String latest = "latest";

	static String pulseRpc = "https://rpc.pulsechain.com/";

	/** map contract address (lower case) to number of Decimals, so we only query it once;
	 *  this assumes all calls are on the same blockchain */ 
	private HashMap<String,Integer> decMap = new HashMap<>();
	

	/** different nodes have different batch sizes; you can probably get bigger size
	 * with a paid node e.g. Moralis */
	private int maxBatchSize = 20; // default only; size is overriden from config file

	/** you could make this a member var */
	private String rpcUrl;  // with or without trailing /; note you can get your very own rpc url from Moralis for more bandwidth
	private int chainId;
	
	/** note that we sometimes pass rpcUrl with trailing / and sometimes not */
	public NodeInstance( String rpcUrlIn, int chainIdIn, int maxBatchSizeIn) throws Exception {
		rpcUrl = rpcUrlIn;
		chainId = chainIdIn;
		
		if (maxBatchSizeIn > 0) {
			maxBatchSize = maxBatchSizeIn;
		}

		S.out( "Creating node instance  rpcUrl=%s  maxBatchSize=%s", rpcUrlIn, maxBatchSize);
	}
	
	/** Send a query and expect "result" to contain a hex value.
	 *  A value of '0x' is invalid and will throw an exception */
	private String queryHexResult( String body, String text, String contractAddr, String walletAddr) throws Exception {
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
	private JsonObject nodeQuery(String body) throws Exception {
		JsonObject obj = (JsonObject)nodeQueryToAny( body);
		
		JsonObject err = obj.getObject( "error");
		if (err != null) {
			throw new MyException( "nodeQuery error  code=%s  %s", err.getInt( "code"), err.getString( "message") );
		}
		return obj;
	}

	public JsonArray batchQuery( JsonArray requests) throws Exception {
		int i = 0;
		var results = new JsonArray();
		
		while (i < requests.size() ) {
			var batch = new JsonArray();
			
			for (int j = 0; j < maxBatchSize && i < requests.size(); j++) {
				batch.add( requests.get( i++) );
			}
			
			smallBatchQuery( batch.toString() )
				.forEach( item -> results.add( item) );
		}
		
		return results;
	}

	/** query size must be <= maxBatchSize */
	private JsonArray smallBatchQuery( String body) throws Exception {
		var anyJson = nodeQueryToAny( body);
		
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
	
	/** result could be JsonObject or JsonArray */
	private JSONAware nodeQueryToAny(String body) throws Exception {
		Util.require( rpcUrl != null, "Set the Moralis rpcUrl");

		return MyClient.create( rpcUrl, body)
				.header( "accept", "application/json")
				.header( "content-type", "application/json")
				.queryToAnyJson();
	}

	/** see also getLatestBlock() */
	public long getBlockNumber() throws Exception {
		String body = """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_blockNumber"
			}""";
		
		long block = nodeQuery( body).getLong( "result");
		Util.require( block > 0, "Error: could not get current block number");
		
		return block;
	}
	
	/** Get the n latest blocks and for each return the gas price that covers
	 *  pct percent of the transactions */
	public JsonObject getFeeHistory(int blocks, int pct) throws Exception {
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
	public JsonObject getQueuedTrans() throws Exception {
		String body = """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "txpool_content"
				}""";
		return nodeQuery( body).getObject("result");  // result -> pending and result -> queued
	}

	public double getNativeBalance(String walletAddr) throws Exception {
		String body = String.format( """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "eth_getBalance",
				"params": [ "%s", "latest" ]
				}""", walletAddr);
		
		return Erc20.fromBlockchain( nodeQuery( body).getString( "result"), 18);
	}

	public Fees queryFees() throws Exception {
		// params are # of blocks, which percentage to look at
		JsonObject json = getFeeHistory(5, 1).getObject( "result");

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
	
	/** show pending and queued transactions to find stuck transactions
	 *  take a long time and returns a lot of data
	 *  
	 *  Goes with CancelStuckTransaction class */
	public void showTrans( String wallet) throws Exception {
		JsonObject result = getQueuedTrans();

		S.out( "Pending");
		show( result.getObject( "pending"), wallet);
		
		S.out( "");
		S.out( "Queued");
		show( result.getObject( "queued"), wallet);
	}
	
	// I think the issue is that you have pending trans that will never get picked up
	// and they are blocking next trans; they have to be removed
	private static void show( JsonObject obj, String addr) throws Exception {
		obj.getObjectNN( Keys.toChecksumAddress(addr) ).display();
	}

	/** see also getBlockNumber() */
	public JsonObject getLatestBlock() throws Exception {
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
	public double getTotalSupply(String contractAddr, int decimals) throws Exception {
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
	
	public double getAllowance( String contractAddr, String approverAddr, String spenderAddr, int decimals) throws Exception {
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
	
	/** Return number of decimals for the contract; first time sends a query,
	 *  subsequent times are map lookup
	 *  
	 *  never called*/
	public synchronized int getDecimals(String contractAddr) throws Exception {
		return Util.getOrCreateEx( decMap, contractAddr.toLowerCase(), () ->
			getTokenDecimals( contractAddr.toLowerCase() ) );
	}

	/** allow the user to pre-fill the decimals map to avoid sending queries when possible */
	public void setDecimals(Erc20 token) {
		setDecimals( token.decimals(), Util.toArray(token.address() ) ); 
	}

	/** allow the user to pre-fill the decimals map to avoid sending queries when possible */
	public synchronized void setDecimals(int decimals, String[] addresses) {
		S.out( "Pre-filling decimals map  decimals=%s  count=%s", decimals, addresses.length);
		
		for (var address : addresses) {
			decMap.put( address.toLowerCase(), decimals);
		}
	}
	
//	public long queryDecimals(String contractAddr) throws Exception {
//		var p1 = Util.toJson( "to", contractAddr, "data", "0x18160ddd"); 
//		var req = new Req( "eth_call", 1)
//				.append( "params", Util.toArray( p1, "latest") );
//		return Util.getLong( queryHexResult( req.toString(), "getDecimals", contractAddr, "n/a") );
//	}

	public int getTokenDecimals(String contractAddr) throws Exception {
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
	public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
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
	public double getBalance( String contractAddr, String walletAddr, int decimals) throws Exception {
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
		
	/** @return null if no receipt; must handle the no receipt or not ready yet state */
	public JsonObject getReceipt( String transHash) throws Exception {
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_getTransactionReceipt",
			"params": [ "%s" ]
			}""", transHash);
		
		// no result is no "result" tag, just id
		return nodeQuery( body).getObject( "result");
	}
	
	public boolean isKnownTransaction(String transHash) throws Exception {
		return getTransactionByHash( transHash) != null;
	}
	
	/** returns the 'result' or null if not found 
	 *  fields are: blockHash, accessList, transactionIndex, type, nonce, input, r, s, chainId, v, blockNumber, gas, maxPriorityFeePerGas, from, to, maxFeePerGas, value, hash, gasPrice */
	public JsonObject getTransactionByHash( String transHash) throws Exception {
		String body = String.format( """
				{
				"jsonrpc": "2.0",
				"id": 1,
				"method": "eth_getTransactionByHash",
				"params": [ "%s" ]
				}""", transHash);
			
		return nodeQuery( body).getObject( "result");
	}
	
	public long getBlockNumber( String transHash) throws Exception {
		var obj = getTransactionByHash( transHash);
		Util.require( obj != null, "Transaction not found with hash " + transHash);
		
		long block = obj.getLong( "blockNumber");
		Util.require( block > 0, "Block number not found for transaction " + transHash);
		
		return block;
	}
	
	/** returns transaction age in blocks */
	public long getTransactionAge( String transHash) throws Exception {
		return getBlockNumber() - getBlockNumber( transHash);
	}
	
	/** This is a receipt for an ERC-20 token transfer */
	public static record Transfer( String contract, String from, String to, double amount, long block, String hash) {
	}
	
	public static class Transfers extends ArrayList<Transfer> {
	}

	public Transfer getTransferReceipt( String hash, int decimals) throws Exception {
		var receipt = getReceipt( hash);
		Util.require( receipt != null && receipt.getString( "status").equals( "0x1"), "Receipt is invalid");
		
	    // Loop through all logs in the receipt to find Transfer events
	    for (var log : receipt.getArray( "logs") ) {
	    	ArrayList<String> topics = log.<String>getArrayOf( "topics");
	    	
	    	if (topics.size() >= 3 && topics.get( 0).equals( transferEventSignature) ) {
	    		String contract = log.getString( "address");
	    		String sender = "0x" + topics.get( 1).substring(26); // Extract sender from topics[1]
	    		String recipient = "0x" + topics.get( 2).substring(26); // Extract recipient from topics[2]

	    		// The amount of tokens transferred is stored in the 'data' field (hexadecimal value)
	    		double amount = Erc20.fromBlockchain( log.getString( "data"), decimals );

				return new Transfer( contract, sender, recipient, amount, log.getLong( "blockNumber"), hash);
	    	}
	    }
	    return null;
	}

	/** return all token transfers for the specified wallet and contract addresses */
	public Transfers getTokenTransfers( String wallet, String[] addresses) throws Exception {
		// int fromBlock = 20556807;  // def. not good. pas
		String fromBlock = "earliest";

		Transfers trans = new Transfers();
		trans.addAll( getTransfers( addresses, fromBlock, wallet, null) );
		trans.addAll( getTransfers( addresses, fromBlock, null, wallet) );

		return trans;
	}
	
	/** return all token transfers for the specified wallet and contract addresses
	 *  additionally filtered by "from" and "to" wallets */
	private List<Transfer> getTransfers( String[] addresses, String fromBlock, String from, String to) throws Exception {
		var query = NodeAux.createReq( addresses, fromBlock, from, to);
		S.out( "query: " + query);
		
		var json = nodeQuery( query.toString() );
		S.out( "response: " + json);
		
		return NodeAux.processResult( json, this::getDecimals);
	}
	
	public HashMap<String,Double> getHolderBalances( String contract, int decimals) throws Exception {
		HashMap<String,Double> map = new HashMap<>();

		// get all transactions, build the map
		for (var transfer : getAllTokenTransfers( contract, decimals) ) {
			Util.inc( map, transfer.from(), -transfer.amount() );
			Util.inc( map, transfer.to(), transfer.amount() );
		}

		return map;
	}

    // Function to fetch token holders from an Ethereum RPC node
	public Transfers getAllTokenTransfers( String address, int decimals) throws Exception {
		address = address.toLowerCase();
		
		var topics = new ArrayList<String>();
		topics.add(transferEventSignature);

		JsonObject params = new JsonObject();
		params.put("fromBlock", "earliest");
		params.put("toBlock", "latest");
		params.put("address", address);
		params.put("topics", topics);

		JsonArray paramArray = new JsonArray();
		paramArray.add(params);
		
		// build json request
		JsonObject jsonRequest = new JsonObject();
		jsonRequest.put("jsonrpc", "2.0");
		jsonRequest.put("method", "eth_getLogs");
		jsonRequest.put("id", 1);
		jsonRequest.put("params", paramArray);
		
		// send query for logs for the specified contract address; could be too huge for popular tokens
		JsonObject jsonResponse = nodeQuery( jsonRequest.toString() );

		Transfers ts = new Transfers();
		for (var log : jsonResponse.getArray("result") ) {
			Util.iff( NodeAux.parseLog( log, decimals), transfer -> ts.add( transfer) );
		}

		return ts;
	}
	
	// Assuming Param, Address, and BigInt classes are defined as provided
	public static void main(String[] args) throws Exception {
		Config c = Config.ask();
		var tok = c.readStocks().getAnyStockToken();
		
		Param[] params = {
				new Address( prod),
				new Address( c.rusdAddr()),
				new Address( tok.address()),
				new BigInt( c.rusd().toBlockchain( 1.) ),
				new BigInt( tok.toBlockchain( 1.) )
		};
		
		c.node().callSigned( 
				c.admin1Key(),
				c.rusdAddr(),
				FbRusd.sellStockKeccak,
				params,
				500000
				)
			.waitForReceipt();
	}				

	/** @deprecated use queryFees instead */
	BigInteger getGasPrice() throws Exception {
		return Numeric.decodeQuantity(
				queryHexResult( new Req("eth_gasPrice", 1).toString(), "gas", "n/a", "n/a")
			);
	}
	
	public BigInteger getNonce(String addr) throws Exception {
		var req = new Req("eth_getTransactionCount", 1);
		req.put( "params", Util.toArray( addr, latest) );
		return Erc20.decodeQuantity( queryHexResult( req.toString(), "count", "n/a", "n/a") );
	}	
	
	// to get the reason you have to call again with the "defaultBlockParameter" set to 
	// the block returned set like this
	// DefaultBlockParameter.valueOf(transactionReceipt.getBlockNumber()))
	// as a second parameter in the list of parameters
	

	// Updated method signature to include baseFee, priorityFee, and gasLimit for EIP-1559 transactions
	/** Not used, but could be;
	 *  @return hash code */
	public RetVal callSigned(
			String privateKey, 
			String contractAddr, 
			String keccak, 
			Param[] params, 
			int gasLimit 
			) throws Exception {
		
		String callerAddr = Util.getAddress( privateKey);
		String encodedData = Param.encodeData(keccak, params);
		var fees = queryFees();
		
		RawTransaction rawTransaction = RawTransaction.createTransaction(
				chainId,
				getNonce(callerAddr),
				BigInteger.valueOf( gasLimit),
				contractAddr,
				BigInteger.ZERO,  // no ether to send
				encodedData,
				fees.priorityFee(),
				fees.totalFee()
				);
		
		Credentials credentials = Credentials.create(privateKey);
		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
		String hex = Numeric.toHexString(signedMessage);
		
		Req req = new Req("eth_sendRawTransaction", 1);
		req.put( "params", Util.toArray( hex) );
		
		String hash = queryHexResult(req.toString(), "signedTransaction", contractAddr, callerAddr);
		S.out( "  transaction hash: " + hash);
		
		return new NewRetVal( hash, this, callerAddr, contractAddr, encodedData);
	}

	public void getRevertReason( String from, String to, String keccak, Param[] params) throws Exception {
		getRevertReason( from, to, Param.encodeData( keccak, params), "latest");
	}
	
	public void getRevertReason(String from, String to, String data, String blockNumber) throws Exception {
		Req req = new Req( "eth_call", 1);

		var param1 = Util.toJson(
			"from", from,
			"to", to,
			"data", data,
			"value", "0x0",
			"gas", "0x1"
			);
//		"gas": "0x7a120"  // (optional) Gas limit (500,000 in this case)
		
		req.put( "params", Util.toArray( param1, blockNumber) );
		S.out( "revert reason is ");
		nodeQuery( req.toString() ).display();
	}

	// Method to encode the function call with its parameters
	// remove, redundant
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


// notes
// you can use eth_call to get revert reason for a past block AND ALSO to see what would
// happen if you called the transaction now, on the current block
// probably you should do that before each call, then you don't need to check all
// the dif params, and 