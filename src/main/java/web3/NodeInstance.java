package web3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONAware;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import common.Util;
import http.MyClient;
import refblocks.Refblocks;
import test.MyTimer;
import tw.util.MyException;
import tw.util.S;
import web3.RetVal.NodeRetVal;

/** id sent will be returned on response; you could batch all different query types
 *  when it gets busy */
public class NodeInstance {
    static final String transferEventSignature = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
	public static String prod = "0x2703161D6DD37301CEd98ff717795E14427a462B".toLowerCase();
	public static final String nullAddr = "0x0000000000000000000000000000000000000000";
	public static final String latest = "latest";
	public static final String pending = "pending";

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
		S.out( "NODE %s  contract='%s'  wallet='%s'", text, contractAddr, walletAddr);
		var json = nodeQuery( body);
		String result = json.getString( "result");
		
		if (result.equals( "0x") ) {
			json.display();
			throw new MyException( "Could not get %s; contractAddr '%s' may be invalid", text, contractAddr);
		}

		return result;
	}

	/** This can be a node created for you on Moralis, which you pay for, or a free node.
	 *  Currently using the free node; if you hit pacing limits, switch to the Moralis node.
	 *  The Moralis node requires auth data 
	 *  
	 * note that we sometimes pass rpcUrl with trailing / and sometimes not */
	private JsonObject nodeQuery(JsonObject json) throws Exception {
		return nodeQuery( json.toString() );
	}
	
	private JsonObject nodeQuery(String body) throws Exception {
		return nodeQuery( body, 0);
	}

	/** @param timeout in seconds; pass zero to use default */
	private JsonObject nodeQuery(String body, int timeoutSec) throws Exception {
		JsonObject obj = (JsonObject)nodeQueryToAny( body, timeoutSec);
		
		JsonObject err = obj.getObject( "error");
		if (err != null) {
			throw new MyException( "nodeQuery error  code=%s  chain=%s  %s", 
					err.getInt( "code"), chainId, err.getString( "message") );
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
		var anyJson = nodeQueryToAny( body, 0);
		
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
	
	/** result could be JsonObject or JsonArray 
		@param timeout in seconds; pass zero to use default */
	private JSONAware nodeQueryToAny(String body, int timeoutSec) throws Exception {
		Util.require( rpcUrl != null, "Set the Node RPC URL");
		
		return MyClient.create( rpcUrl, body)
				.header( "accept", "application/json")
				.header( "content-type", "application/json")
				.timeout( timeoutSec)
				.queryToAnyJson();
	}

	/** see also getLatestByNumber */
	public String getBlockDateTime( long block) throws Exception {
		long sec = getBlockByNumber( block).getLong( "timestamp");
		return Util.yToS.format( new Date( sec * 1000));
	}
	
	/** see also getLatestBlock and getBlockDateTime */
	public JsonObject getBlockByNumber( long block) throws Exception {
		return getBlockBy( Util.toHex( block) );
	}

	/** Gets the entire latest block */
	public JsonObject getLatestBlock() throws Exception {
		return getBlockBy( "latest");
	}
	
	/** @param str can be 'latest' or block number in hex */ 
	public JsonObject getBlockBy( String str) throws Exception {
		Util.require( str.equals( "latest") || str.startsWith( "0x"), "invalid block number");
		
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_getBlockByNumber",
			"params": ["%s", false]
			}""", str);
		return nodeQuery( body).getObject( "result"); 
	}

	/** Gets just the latest block number */
	public long getBlockNumber() throws Exception {
		var body = new Req( "eth_blockNumber");
		long block = nodeQuery( body).getLong( "result");
		Util.require( block > 0, "Error: could not get current block number");
		
		return block;
	}
	
	/** Get the n latest blocks and for each return the gas price that covers
	 *  pct percent of the transactions
	 *  
	 *  Note that some rows can contain zero */
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

	/** Get the n latest blocks; note that some blocks can contains all zeros */
	public JsonObject getFullFeeHistory(int blocks) throws Exception {
		String body = String.format( """
			{
			"jsonrpc": "2.0",
			"id": 1,
			"method": "eth_feeHistory",
			"params": [ "%s", "latest", [ 10, 30, 50, 70, 90] ]
			}""", blocks); 
		return nodeQuery( body);
	}

	/** This takes a long time and returns a lot of data.
	 *  There are two lists returned: Pending and Queued.
	 *  The Queued is not reliable because it is different for each node
	 *  and when using an RPC node (aka node aggregator), you never know
	 *  which node your query is going to go to. */
	public JsonObject getQueuedTrans() throws Exception {
		var body = new Req( "txpool_content"); // frequently does not work and returns 'panic recovery', no idea why
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
		return queryFees( 6, 60);
	}
	
	private Fees queryFees( int blocks, int pct) throws Exception {  // this version for testing only
		// params are # of blocks, which percentage to look at
		JsonObject json = getFeeHistory(blocks, pct).getObject( "result");

		// get base fee of last/pending block
		long baseFee = Util.getLong( json.<String>getArrayOf( "baseFeePerGas").get( 0) );

		// take average of median priority fee over last 5 blocks 
		long sum = 0;
		double count = 0;
		ArrayList<ArrayList> reward = json.<ArrayList>getArrayOf( "reward");
		for ( ArrayList ar : reward) {
			long val = Util.getLong( ar.get(0).toString() );
			if (val > 0) {   // on some chains (e.g. amoy), some rows contain all zeros; these rows must be ignored
				sum += val;
				count++;
			}
		}

		return new Fees( baseFee * 1.2, sum / count);
	}
	
	/** show pending and queued transactions to find stuck transactions
	 *  take a long time and returns a lot of data
	 *  
	 *  Goes with CancelStuckTransaction class */
	public void showTrans( String wallet) throws Exception {
		S.out( "Transactions");

		JsonObject result = getQueuedTrans();
		
		S.out( "Types: " + result.getKeys() );

		S.out( "Pending");
		var pending = result.getObject( "pending");
		show( pending, wallet);
		
		S.out( "");
		S.out( "Queued");
		var queued = result.getObject( "queued"); 
		show( queued, wallet);
	}
	
	// I think the issue is that you have pending trans that will never get picked up
	// and they are blocking next trans; they have to be removed
	private static void show( JsonObject obj, String wallet) throws Exception {
		var mine = obj.getObjectNN( Keys.toChecksumAddress(wallet) );
		var keys = mine.getKeys();
		keys.sort( null);
		for (var key : keys) {
			S.out( key);
		}
		//mine.display();
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
	
	/** returns allowance in hex */
	public String getAllowance( String contractAddr, String approverAddr, String spenderAddr) throws Exception {
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
		
		return queryHexResult( body, "allowance", contractAddr, approverAddr);
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
	public void setDecimals(int decimals, String address) {
		decMap.put( address.toLowerCase(), decimals);
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
		Req( String method) {
			this( method, 1);
		}

		Req( String method, JsonObject param) {
			this( method, 1);
			put( "params", Util.toArray( param) ); // must be an arrar
		}
		
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
	
	public String getOwner( String contractAddr) throws Exception {
		Req req = new Req( "eth_call");
		req.put( "params", Util.toArray( 
				Util.toJson( "to", contractAddr, "data", "0x8da5cb5b"),
				"latest")
				);
		String str = queryHexResult( req.toString(), "getOwner", contractAddr, "n/a");
		return "0x" + Util.right( str, 40);
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
	public static record Transfer( String contract, String from, String to, double amount, long block, String hash, String timestamp) {
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

				return new Transfer( contract, sender, recipient, amount, log.getLong( "blockNumber"), hash, "");
	    	}
	    }
	    return null;
	}

	/** return all token transfers for the specified wallet and contract addresses */
	public Transfers getWalletTransfers( String wallet, String[] addresses) throws Exception {

		Transfers trans = new Transfers();
		trans.addAll( getTransfers( addresses, wallet, null) );
		trans.addAll( getTransfers( addresses, null, wallet) );

		return trans;
	}
	
    /** return all transfers for the specified token address */
	public Transfers getTokenTransfers( String address) throws Exception {
		return getTransfers( Util.toArray( address), null, null);
	}

	final long chunks = 1;
	final int chunkTimeoutSec = 45;

	/**
	 * could break up the query into multiple queries; this works, but it seems that
	 * increasing the timeout also works and is a simpler solution, so the chunk
	 * code is not being used presently
	 * 
	 * Another way to break it down would be to send a separate query for each address
	 * 
	 * This query can return wrong results; if so, try again. You need a paid 'archival'
	 * node to be more accurate, or to run your own archival node.
	 * 
	 * return all token transfers for the specified wallet and contract addresses
	 *  additionally filtered by "from" and "to" wallets
	 *  @param from may be null
	 *  @param to may be null */
	private Transfers getTransfers( String[] addresses, String from, String to) throws Exception {
		// set up filtering by to OR from addresses (one or both will be null)
		var topics = new ArrayList<String>();
		topics.add( NodeInstance.transferEventSignature);
		topics.add( NodeAux.pad( from) );
		topics.add( NodeAux.pad( to) );

		// build params to filter to contract addresses
		JsonObject params = new JsonObject();
		params.put("topics", topics);
		params.put("address", addresses);  // filter by contract addresses

		// break the query into chunks? chunks could be one
		long num = chunks > 1 ? getBlockNumber() : 1;
		long chunkSize = num / chunks; 
		long start = 0;
		long end = chunkSize;
		
		Transfers transactions = new Transfers();

		for (int chunk = 1; chunk <= chunks; chunk++) {
			// set from/to blocks in parameters
			params.put("fromBlock", chunk == 1 ? "earliest" : "" + start);
			params.put("toBlock", chunk == chunks ? "latest" : "" + end);
			
			// query for logs for one chunk
			MyTimer t = new MyTimer().start();
			JsonObject query = new Req("eth_getLogs", params);
			S.out( "NODE getTrans  chunk=%s  from=%s  to=%s", chunk, params.getString( "fromBlock"), params.getString( "toBlock") );
			var results = nodeQuery( query.toString(), chunkTimeoutSec).getArray("result");
			S.out( "  returned %s logs in %s ms", results.size(), t.time() );
			
			// convert result to array of Transfers
			for (var log : results) {
				Util.iff( NodeAux.parseLog( log, getDecimals( log.getString("address") ) ), 
						transfer -> transactions.add( transfer) ); 
			}

			start = end + 1;
			end += chunkSize;
		}
		
		return transactions;
	}
	
	/** @deprecated use queryFees instead */
	BigInteger getGasPrice() throws Exception {
		return Numeric.decodeQuantity(
				queryHexResult( new Req("eth_gasPrice").toString(), "gas", "n/a", "n/a")
			);
	}
	
	public BigInteger getNonce(String wallet) throws Exception {
		var req = new Req("eth_getTransactionCount");
		req.put( "params", Util.toArray( wallet, latest) );
		return Erc20.decodeQuantity( queryHexResult( req.toString(), "nonce", "n/a", wallet) );
	}	

	public BigInteger getNoncePending(String wallet) throws Exception {
		var req = new Req("eth_getTransactionCount");
		req.put( "params", Util.toArray( wallet, pending) );
		return Erc20.decodeQuantity( queryHexResult( req.toString(), "nonce", "n/a", wallet) );
	}	
	
	// to get the reason you have to call again with the "defaultBlockParameter" set to 
	// the block returned set like this
	// DefaultBlockParameter.valueOf(transactionReceipt.getBlockNumber()))
	// as a second parameter in the list of parameters
	

	// Updated method signature to include baseFee, priorityFee, and gasLimit for EIP-1559 transactions
	public RetVal callSigned(
			String privateKey, 
			String contractAddr, 
			String keccak, 
			Param[] params, 
			long gasLimit 
			) throws Exception {
		
		return callSigned(
				privateKey,
				contractAddr,
				Param.encodeData(keccak, params),
				gasLimit);
	}

	/** works for all chains but not zkSync */
	public String deploy( String privateKey, String byteCode, String params) throws Exception {
		Util.require( !params.startsWith( "0x"), "invalid params");

		String hash = callSigned( privateKey, "", byteCode + params, Refblocks.deployGas)
				.waitForReceipt();
		
		var receipt = getReceipt( hash);
		
		String address = receipt.getString( "contractAddress");
		Util.require( S.isNotNull( address), "Error: no contract address returned");
		
		return address;
	}
	
	public RetVal callSigned( String privateKey, String contractAddr, String data, long gasLimit) throws Exception {		
		String callerAddr = Util.getAddress( privateKey);
		
		var fees = queryFees();
		//fees.display( BigInteger.valueOf( gasLimit) );
		var nonce = getNonce(callerAddr);
		
		RawTransaction rawTransaction = RawTransaction.createTransaction(
				chainId,
				nonce,
				BigInteger.valueOf( gasLimit),
				contractAddr,
				BigInteger.ZERO,  // no ether to send
				data,
				fees.priorityFee(),
				fees.totalFee()
				);
		
		Credentials credentials = Credentials.create(privateKey);
		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
		String signedHex = Numeric.toHexString(signedMessage);
		
		Req req = new Req("eth_sendRawTransaction");
		req.put( "params", Util.toArray( signedHex) );
		
		String hash = queryHexResult(req.toString(), "signedTransaction  nonce=" + nonce, contractAddr, callerAddr);
		S.out( "  transaction hash: " + hash);
		
		return new NodeRetVal( hash, this, callerAddr, contractAddr, data, gasLimit);
	}
	
	public void preCheck(String from, String to, String keccak, Param[] params, int gasLimit) throws Exception {
		Util.reqValidAddress(from);
		Util.reqValidAddress(to);
		Util.require( keccak.startsWith( "0x"), "Invalid keccak");
		
		var fees = queryFees();

		var param1 = Util.toJson(
			"from", from,
			"to", to,
			"data", Param.encodeData(keccak, params),
			"value", "0x0",
			"gas", Util.toHex( gasLimit),  // gas limit
			"maxFeePerGas", Util.toHex( fees.totalFee() ),
			"maxPriorityFeePerGas", Util.toHex( fees.priorityFee() )
			);
		
		Req req = new Req( "eth_call");
		req.put( "params", Util.toArray( param1, "latest") );

		var result = nodeQuery( req.toString() ); // throws an exception if it fails 
		S.out( "Pre-check  from=%s  to=%s  keccak %s  result=%s",
				from, to, keccak, result);
	}

	public void getRevertReason( String from, String to, String keccak, Param[] params, long gasLimit) throws Exception {
		getRevertReason( from, to, Param.encodeData( keccak, params), gasLimit, "latest");
	}
	
	public void getRevertReason(String from, String to, String data, long gasLimit, String blockNumber) throws Exception {
		Req req = new Req( "eth_call");

		var param1 = Util.toJson(
			"from", from,
			"to", to,
			"data", data,
			"value", "0x0",
			"gas", Util.toHex( gasLimit)
			);
		
		req.put( "params", Util.toArray( param1, blockNumber) );
		
		// will throw an exception if revert reason is returned
		nodeQuery( req.toString() );

		// could not get reason
		throw new Exception( "Could not get revert reason");
	}
	
}

// looking for transferNativeToken()? see RefBlocks.transfer(); we could add it here but it has to be signed

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