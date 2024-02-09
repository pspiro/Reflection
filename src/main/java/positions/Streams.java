package positions;

import static org.junit.jupiter.api.DynamicTest.stream;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import tw.util.S;

// Q: does JsonObject translate // back and forth to \/\/?
// Q my question is, if allAddresses is set to false, how do I specify which contract it listens to?

public class Streams {
	public static void main(String[] args) throws Exception {
//		listen();
		//deleteAll();

//		S.out( "Creating stream");
//		createStream(erc20Transfers);
		
//		addAddressToStream( id, "0x7a248d1186e32a06d125d90abc86a49e89730d74");
		
		displayStreams();
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
			server.createContext("/webhook", exch -> new Trans(exch).handleWebhook() );
			server.createContext("/", exch -> new Trans(exch).respondOk() );
		});
	}

	private static void addAddressToStream(String id, ArrayList<String> list) throws Exception {
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
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=2");
		int total = obj.getInt("total");
		JsonArray ar = obj.getArray("result");
		
		for (JsonObject stream : ar) {
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

	/** Create the stream, add all addresses, and activate it */
	static void createStream(String json, ArrayList<String> contracts) throws Exception {
		String id = createStream(json);
		addAddressToStream(id, contracts);
		setStatus( id, true);
	}

	private static String createStream(String json) throws Exception {
		//JsonObject json = JsonObject.parse( body);
		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json) );
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
         "description" : "Monitor ERC20 transfers",
         "getNativeBalances" : [ ],
         "triggers" : [ ],
         "webhookUrl" : "http://108.6.23.121/hook/webhook",
         "includeContractLogs" : true,
         "includeAllTxLogs" : false,
         "includeInternalTxs" : false,
         "allAddresses" : false,
         "tag" : "hello",
         
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
         ],
         
         "chainIds" : [
            "0x5"
         ]
	}
	""";
	
	static String nativeTrans = """
	{
         "description" : "Monitor native transactions",
         "getNativeBalances" : [ ],
         "triggers" : [ ],
         "webhookUrl" : "http://108.6.23.121/hook/webhook",
         "includeContractLogs" : true,
         "includeAllTxLogs" : false,
         "allAddresses" : false,
         "includeInternalTxs" : false,
         "tag" : "hello",
         
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
         ],
         
         "chainIds" : [
            "0x5"
         ]
	}
	""";
}
