package positions;


import java.io.IOException;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import reflection.Config;
import tw.util.S;

// Q: does JsonObject translate // back and forth to \/\/?
// Q my question is, if allAddresses is set to false, how do I specify which contract it listens to?

public class Streams {

	public static void main(String[] args) throws Exception {
		Config.ask();
		
//		listen();
		deleteAll();
//
//		createNative();

//		S.out( "Creating stream");
//		createStream(erc20Transfers);
		
//		addAddressToStream( id, "0x7a248d1186e32a06d125d90abc86a49e89730d74");
	
		//createNative();
		
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
	
	private static void listen() throws IOException {
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch).handleWebhook() );
			server.createContext("/", exch -> new Trans(exch).respondOk() );
		});
	}

	static void addAddressToStream(String id, String... list) throws Exception {
		if (list.length > 0) {
			S.out( "Adding addresses [%s] to stream %s", String.join(", ", list), id);
			
		     MoralisServer.post(
		    		 String.format( "https://api.moralis-streams.com/streams/evm/%s/address", id), 
		    		 Util.toJson( "address", list).toString() );
		}
	}
	
	public static void deleteAll() throws Exception {		
		MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
			.getArray("result").forEach( stream -> Util.wrap( () ->
				deleteStream( stream.getString("id") ) ) );
	}
	
	static void displayStreams() throws Exception {
		S.out( "Existing streams");
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=5");
		int total = obj.getInt("total");
		JsonArray ar = obj.getArray("result");
		
		for (JsonObject stream : ar) {
//			stream.display();
			S.out( "Stream " + stream.getString("description") );
			S.out( stream);
			displayAddresses( stream.getString("id") );
		}
	}

	private static void displayAddresses(String id) throws Exception {
		JsonObject obj = MoralisServer.queryObject( 
			String.format( "https://api.moralis-streams.com/streams/evm/%s/address?limit=5", id) );
		S.out( "  " + obj);
	}

	/** @return stream id */
	static String createStreamWithAddresses(String json, String... contracts) throws Exception {
		json = JsonObject.parse( json).toString();  // very weird and annoying--only the "approvals" stream breaks without this!

		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json.toString() ) );
		String id = obj.getString("id");

		addAddressToStream(id, contracts);
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
