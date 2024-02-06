package positions;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import tw.util.S;

// Q: does JsonObject translate // back and forth to \/\/?
// Q my question is, if allAddresses is set to false, how do I specify which contract it listens to?

public class TestServ {
	public static void main(String[] args) throws Exception {
		display();
		
		listen();
		
		String id = create();

		addAddress( id, "0x7a248d1186e32a06d125d90abc86a49e89730d74");

		display();
		
		System.exit(0);
	}
	
	private static void addAddress(String id, String address) throws Exception {
		S.out( "Adding address %s to %s", address, id);
	     String resp = MoralisServer.post(
	    		 String.format( "https://api.moralis-streams.com/streams/evm/%s/address", id), 
	    		 Util.toJson( "address", address).toString() );
	     S.out( resp);
	}

	static void listen() throws Exception {
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/", exch -> new Trans(exch).handle() );
		});
	}
	
	static void display() throws Exception {
		S.out( "Existing stream");
		JsonObject obj = MoralisServer.queryObject( "https://api.moralis-streams.com/streams/evm?limit=1");
		obj.display();
	}
	
	static String create() throws Exception {
		JsonObject json = JsonObject.parse(body);

		S.out( "Creating stream");
		JsonObject obj = JsonObject.parse(
				MoralisServer.put( "https://api.moralis-streams.com/streams/evm", json.toString() ) );
		obj.display();

		return obj.getArray("result").get(0).getString("id");
	}

	static class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange) {
			super(exchange, true);
		}

		public void handle() {
			wrap( () -> {
				parseToObject().display();
				respondOk();
			});
		}
	}
	static String body = """
	{
         "description" : "Stream 1",
         "getNativeBalances" : [ ],
         "triggers" : [ ],
         "webhookUrl" : "http://108.6.23.121/webhook",
         "includeContractLogs" : true,
         "includeAllTxLogs" : false,
         "allAddresses" : true,
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
