package testcase;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.BaseTransaction;
import http.MyClient;
import http.MyServer;
import tw.util.S;

public class TestAlchemy {
	public static void main(String[] args) throws Exception {
		MyServer.listen( 8080, 10, server -> {
			server.createContext("/", exch -> new Tr( exch).handle() );
		});
	
//	     --header 'accept: application/json' \
//	     --header 'content-type: application/json' \

		// try MATIC_MAINNET
		// ZKSYNC_MAINNET
		String body = """
			{
			"network": "MATIC_MAINNET", 
			"webhook_type": "ADDRESS_ACTIVITY",
			"webhook_url": "https://85d5-108-41-13-219.ngrok-free.app",
			"addresses": [
			]
			}""";
		
		String str = MyClient.create( "https://dashboard.alchemy.com/api/create-webhook", body) 
			.header( "accept", "application/json")
			.header( "content-type", "application/json")
			.header( "X-Alchemy-Token", "K9VYjc0AdzyVJjCb5dpaybSiEDlLKV5h")
			.query()
			.body();
		var json = JsonObject.parse( str);
		json.display();
	}
	
	static class Tr extends BaseTransaction {
		public Tr(HttpExchange exch) {
			super( exch, true);
		}

		private void handle() {
			Util.wrap( () -> {
				
				if (m_exchange.getRequestBody().available() == 0) {
					out( "  (no data)");
				}
				else {
					handleHookWithData();
				}
				respondOk();
				
			});
		}

		private void handleHookWithData() throws Exception {
			S.out( "-----");
			JsonObject obj = parseToObject();
			obj.display();
		}
	}
}
/*
MATIC transfer event part 1
{
"createdAt" : "2024-08-24T03:31:14.552Z",
"webhookId" : "wh_uza3gdn51w0r64vc",
"id" : "whevt_ovw33y55ht4hcsy5",
"type" : "ADDRESS_ACTIVITY",
"event" : {
   "activity" : [
      {
         "rawContract" : {
            "rawValue" : "0x5af3107a4000",
            "decimals" : 18
         },
         "fromAddress" : "0x2703161d6dd37301ced98ff717795e14427a462b",
         "blockNum" : "0x3a245b0",
         "asset" : "MATIC",
         "category" : "external",
         "toAddress" : "0x6117a8a8df7db51662e9555080ab8def0e11c4d3",
         "value" : 0.0001,
         "hash" : "0x11e7ebca86e5d3be46d3466ce8a53e033d843584c7e3361d2be9caa56daba9cc"
      }
   ],
   "network" : "MATIC_MAINNET"
}
}



token transfer event part 1
{
   "createdAt" : "2024-08-24T03:33:33.841Z",
   "webhookId" : "wh_uza3gdn51w0r64vc",
   "id" : "whevt_nio781bm1sakr1ii",
   "type" : "ADDRESS_ACTIVITY",
   "event" : {
      "activity" : [
         {
            "rawContract" : {
               "address" : "0xc2132d05d31c914a87c6611c10748aeb04b58e8f",
               "rawValue" : "0x0000000000000000000000000000000000000000000000000000000000000064",
               "decimals" : 6
            },
            "log" : {
               "blockHash" : "0xc3a668eede33ab0842d17e488b3f49e0b0ac35a05a71c1b5036ba2f5b38a01ce",
               "address" : "0xc2132d05d31c914a87c6611c10748aeb04b58e8f",
               "logIndex" : "0x1a6",
               "data" : "0x0000000000000000000000000000000000000000000000000000000000000064",
               "removed" : false,
               "topics" : [
                  "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef", "0x0000000000000000000000002703161d6dd37301ced98ff717795e14427a462b", "0x0000000000000000000000006117a8a8df7db51662e9555080ab8def0e11c4d3"
               ],
               "blockNumber" : "0x3a245f1",
               "transactionIndex" : "0x55",
               "transactionHash" : "0xee27e0319c34741443e9fd3d2dafede1228551ff56f4a88a9537d0bbe7dfabe7"
            },
            "fromAddress" : "0x2703161d6dd37301ced98ff717795e14427a462b",
            "blockNum" : "0x3a245f1",
            "asset" : "USDT",
            "category" : "token",
            "toAddress" : "0x6117a8a8df7db51662e9555080ab8def0e11c4d3",
            "value" : 0.0001,
            "hash" : "0xee27e0319c34741443e9fd3d2dafede1228551ff56f4a88a9537d0bbe7dfabe7"
         }
      ],
      "network" : "MATIC_MAINNET"
   }
}

*/
