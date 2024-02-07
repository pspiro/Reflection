package positions;

import java.util.ArrayList;

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
		listen();
		
		deleteAll();
		
		String id = createStream();
		addAddressToStream( id, "0x7a248d1186e32a06d125d90abc86a49e89730d74");
		setStatus( id, true);
		
//		//displayStream();
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

	private static void addAddressToStream(String id, String address) throws Exception {
		S.out( "Adding address %s to %s", address, id);
	     String resp = MoralisServer.post(
	    		 String.format( "https://api.moralis-streams.com/streams/evm/%s/address", id), 
	    		 Util.toJson( "address", address).toString() );
	     S.out( resp);
	}
	
	public static void deleteAll() throws Exception {		
		MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=10")
			.getArray("result").forEach( stream -> Util.wrap( () ->
				deleteStream( stream.getString("id") ) ) );
				//setStatus( stream.getString("id"), false) ) );
	}
	
	static void displayStream() throws Exception {
		S.out( "Existing stream");
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=1");
		obj.display();
	}

	/** Create the stream, add all addresses, and activate it */
	static void createStream(ArrayList<String> contracts) throws Exception {
		String id = createStream();
		for (String address : contracts) {
			addAddressToStream(id, address);
		}
		setStatus( id, true);
	}

	/** This does not activate the stream */
	static String createStream() throws Exception {
		JsonObject json = JsonObject.parse(body);

		S.out( "Creating stream");
		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json.toString() ) );

		return obj.getString("id");
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
				obj.getArray("erc20Transfers").forEach( trans -> {
					S.out( "Transferred %s %s", trans.getString("contract"), obj.getBool("confirmed") );
					S.out( "From: %s to %s", trans.getString("from"), trans.getString("to") );
					S.out();
				});
				respondOk();
			});
		}
	}

	static void listen() throws Exception {
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/webhook", exch -> new Trans(exch).handleWebhook() );
			server.createContext("/", exch -> new Trans(exch).respondOk() );
		});
	}

	static String body = """
	{
         "description" : "Stream 1",
         "getNativeBalances" : [ ],
         "triggers" : [ ],
         "webhookUrl" : "http://108.6.23.121/webhook",
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


/* sample stream returned in query

{
nftApprovals : {
   ERC721 : [ ],
   ERC1155 : [ ]
},
streamId : "33d4086a-bbdd-4eec-86cc-9670350577bd",
nftTransfers : [ ],
abi : [
   {
      inputs : [
         {
            indexed : true,
            name : "from",
            type : "address"
         }, {
            indexed : true,
            name : "to",
            type : "address"
         }, {
            indexed : false,
            name : "value",
            type : "uint256"
         }
      ],
      name : "Transfer",
      anonymous : false,
      type : "event"
   }
],
txsInternal : [ ],
erc20Approvals : [ ],
confirmed : false,
txs : [ ],
retries : 0,
nftTokenApprovals : [ ],
chainId : "0x5",
nativeBalances : [ ],
erc20Transfers : [
   {
      logIndex : "54",
      tokenSymbol : "BUSD",
      tokenDecimals : "6",
      contract : "0x7a248d1186e32a06d125d90abc86a49e89730d74",
      possibleSpam : false,
      tokenName : "Ref BUSD",
      valueWithDecimals : "1",
      from : "0x96531a61313fb1bef87833f38a9b2ebaa6ea57ce",
      to : "0x2703161d6dd37301ced98ff717795e14427a462b",
      value : "1000000",
      transactionHash : "0xf3f4b443e5408fcacf425ec8eb6010381d2ae639ff5c06ba3c7ffb2ee904cadb"
   }
],
block : {
   number : "10496647",
   hash : "0x75a195c9e596aff671fcc2a5587eb5832d7abf8acdbbd38393c7c8c0dd4cc8d2",
   timestamp : "1707261504"
},
tag : "hello",
logs : [
   {
      topic1 : "0x00000000000000000000000096531a61313fb1bef87833f38a9b2ebaa6ea57ce",
      topic2 : "0x0000000000000000000000002703161d6dd37301ced98ff717795e14427a462b",
      logIndex : "54",
      address : "0x7a248d1186e32a06d125d90abc86a49e89730d74",
      topic0 : "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
      data : "0x00000000000000000000000000000000000000000000000000000000000f4240",
      transactionHash : "0xf3f4b443e5408fcacf425ec8eb6010381d2ae639ff5c06ba3c7ffb2ee904cadb"
   }
]
}
*/