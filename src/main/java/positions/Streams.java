package positions;

import static fireblocks.Accounts.instance;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.Busd;
import fireblocks.MyServer;
import http.BaseTransaction;
import reflection.Config;
import tw.util.S;

// Q: does JsonObject translate // back and forth to \/\/?
// Q my question is, if allAddresses is set to false, how do I specify which contract it listens to?

public class Streams {
	static String webhookUrl = "http://108.6.23.121/hook/webhook";

	public static void main(String[] args) throws Exception {
//		listen();
//		deleteAll();
//
//		createNative();

//		S.out( "Creating stream");
//		createStream(erc20Transfers);
		
//		addAddressToStream( id, "0x7a248d1186e32a06d125d90abc86a49e89730d74");
	
		//createNative();
		testApprovals();
		
//		displayStreams();
//
////
//		deleteStream(id);
//
//		setStreamStatus( id, false);
//		deleteAll();
//
//		System.exit(0);
		S.sleep( 5*60*1000);
	}
	
	static void testApprovals() throws Exception {
		String str = String.format( approval, webhookUrl, "0x5");
		S.out( "string");
		S.out(str);
		S.out();
		S.out("obj");
		S.out( JsonObject.parse(str) );
		JsonObject.parse(str).display();

		Config config = Config.ask();
		createApprovalStream( config.busd().address() );		
		config.busd().approve( 
				instance.getId( "RefWallet"), // called by
				config.rusd().address(), // approving
				100);
		
	}
	
	private static void createNative() throws Exception {
		createStreamWithAddresses(
				String.format( nativeTrans, webhookUrl, "0x5"),
				Arrays.asList( "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce") );
	}

	private static void createApprovalStream(String address) throws Exception {
		createStreamWithAddresses(
				String.format( approval, webhookUrl, "0x5"),
				Arrays.asList( address) );
	}

	private static void listen() throws IOException {
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch).handleWebhook() );
			server.createContext("/", exch -> new Trans(exch).respondOk() );
		});
	}

	private static void addAddressToStream(String id, List<String> list) throws Exception {
		S.out( "Adding addresses %s to stream %s", list, id);
		
	     String resp = MoralisServer.post(
	    		 String.format( "https://api.moralis-streams.com/streams/evm/%s/address", id), 
	    		 Util.toJson( "address", list).toString() );
	     S.out( resp);
	}
	
	public static void deleteAll() throws Exception {		
		MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
			.getArray("result").forEach( stream -> Util.wrap( () ->
				deleteStream( stream.getString("id") ) ) );
				//setStatus( stream.getString("id"), false) ) );
	}
	
	static void displayStreams() throws Exception {
		S.out( "Existing stream");
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=5");
		int total = obj.getInt("total");
		JsonArray ar = obj.getArray("result");
		
		for (JsonObject stream : ar) {
			stream.display();
//			S.out( "Stream " + stream.getString("description") );
//			S.out( stream);
			displayAddresses( stream.getString("id") );
		}
	}

	private static void displayAddresses(String id) throws Exception {
		JsonObject obj = MoralisServer.queryObject( 
			String.format( "https://api.moralis-streams.com/streams/evm/%s/address?limit=5", id) );
		S.out( "  " + obj);
	}

	/** Create the stream, add all addresses, and activate it */
	static void createStreamWithAddresses(String json, List<String> contracts) throws Exception {
		String id = createStream(json);
		addAddressToStream(id, contracts);
		setStatus( id, true);
	}

	private static String createStream(String json) throws Exception {
		// very weird and annoying--only the "approvals" stream breaks without this!
		json = JsonObject.parse( json).toString();
		
		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json.toString() ) );
		String id = obj.getString("id");
		setStatus( id, true);
		return id;
	}

	public static void deleteStream(String id) throws Exception {
		S.out( "Deleting stream " + id);
		String url = "https://api.moralis-streams.com/streams/evm/" + id;
		S.out( MoralisServer.delete( url) );
	}

	public static void setStatus(String id, boolean active) throws Exception {
		S.out( "Setting stream %s status to %s", id, active ? "active" : "paused");
		String url = String.format( "https://api.moralis-streams.com/streams/evm/%s/status", id);
		S.out( MoralisServer.post( url, 
				Util.toJson( "status", active ? "active" : "paused").toString() ) );
	}

	static class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange) {
			super(exchange, true);
		}

		public void handleWebhook() {
			wrap( () -> {
				JsonObject obj = parseToObject();
				S.out( "Received " + obj);
//				obj.getArray("erc20Transfers").forEach( trans -> {
//					S.out( "Transferred %s %s", trans.getString("contract"), obj.getBool("confirmed") );
//					S.out( "From: %s to %s", trans.getString("from"), trans.getString("to") );
//					S.out();
//				});
				respondOk();
			});
		}
	}

	static String erc20Transfers = """
	{
         "description" : "ERC20 transfers",
         "webhookUrl" : "%s",
         "chainIds" : [ "%s" ]
         "tag" : "ERC20 trans",
         "getNativeBalances" : [ ],
         "triggers" : [ ],
         "includeContractLogs" : true,
         "includeAllTxLogs" : false,
         "includeInternalTxs" : false,
         "allAddresses" : false,
         
         "topic0" : [
            "Transfer(address,address,uint256)"
         ],
         
         "abi" : [
            {
               "inputs" : [
                  {
                     "indexed" : true,
                     "name" : "from",
                     "type" : "address"
                  }, {
                     "indexed" : true,
                     "name" : "to",
                     "type" : "address"
                  }, {
                     "indexed" : false,
                     "name" : "value",
                     "type" : "uint256"
                  }
               ],
               "name" : "Transfer",
               "anonymous" : false,
               "type" : "event"
            }
         ]
	}
	""";
	
	
	static String nativeTrans = """
	{
		"description": "Native token transfers",
		"webhookUrl" : "%s",
		"chainIds": [ "%s" ],
		"tag": "native trans",
		"demo": false,
		"includeNativeTxs": true,
		"allAddresses": false,
		"includeContractLogs": false,
		"includeInternalTxs": false,
		"includeAllTxLogs": false
	}
	""";
	
	static String approval = """
	{
		"description" : "Approvals",
		"webhookUrl" : "%s",
		"chainIds" : [ "%s" ],
		"tag" : "approvals",
		"includeNativeTxs" : false,
		"includeContractLogs" : true,
		"includeAllTxLogs" : false,
		"allAddresses" : false,
		"includeInternalTxs" : false,
	
		"topic0" : [
			"Approval(address,address,uint256)"
		],
	
		"abi" : [
			{
				"inputs" : [
					{
						"indexed" : true,
						"name" : "owner",
						"type" : "address",
					},
					{
						"indexed" : true,
						"name" : "spender",
						"type" : "address"
					},
					{
						"indexed" : false,
						"name" : "value",
						"type" : "uint256"
					}
				],
				"name" : "Approval",
				"anonymous" : false,
				"type" : "event"
			}
		]
	}
	""";
}