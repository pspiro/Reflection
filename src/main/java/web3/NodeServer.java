package web3;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JsonObject;
import org.web3j.crypto.Keys;

import common.Util;
import http.MyClient;
import reflection.Config;
import tw.util.MyException;
import tw.util.S;

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
	private static String queryHexResult( String body, String text, String contractAddr) throws Exception {
		S.out( "Querying %s for '%s'", text, contractAddr);
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
		Util.require( rpcUrl != null, "Set the Moralis rpcUrl");

		JsonObject obj = MyClient.create( rpcUrl, body)
				.header( "accept", "application/json")
				.header( "content-type", "application/json")
				.queryToJson();
		
		JsonObject err = obj.getObject( "error");
		if (err != null) {
			throw new MyException( "nodeQuery error  code=%s  %s", err.getInt( "code"), err.getString( "message") );
		}
		return obj;
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

	/** get ERC-20 token balance; see also getNativeBalance() */
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
		
		return Erc20.fromBlockchain( queryHexResult( body, "balance", contractAddr), decimals);
	}
	
	/** note w/ moralis you can also get the token balance by wallet 
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
		
		return Erc20.fromBlockchain( queryHexResult( body, "totalSupply", contractAddr), decimals);
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
		
		return Erc20.fromBlockchain( queryHexResult( body, "allocance", contractAddr), decimals);
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
		
		S.out( "Querying token decimals for %s", contractAddr);
		return Erc20.decodeQuantity( queryHexResult( body, "decimals", contractAddr)).intValue();
    }
	
	/** Makes a separate call for each one */  // this could return TsonObject
	static public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		Util.reqValidAddress( walletAddr);

		HashMap<String, Double> positionsMap = new HashMap<>();

		for (String contractAddr : contracts) {
			positionsMap.put( contractAddr, getBalance( contractAddr, walletAddr, decimals) );
		}

		return positionsMap;
	}

}
