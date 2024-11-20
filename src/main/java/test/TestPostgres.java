package test;

import reflection.Config.MultiChainConfig;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		MultiChainConfig c1 = new MultiChainConfig();
		c1.readFromSpreadsheet("Prod-config");
		S.out( c1.isProduction() );

		MultiChainConfig c2 = new MultiChainConfig();
		c2.readFromSpreadsheet("Dev-config");
		S.out( c2.isProduction() );
		
//		var chain = new Chains().readOne( "PulseChain", false);
//		S.out( chain.params().getWebhookUrlSuffix() );
//		S.out( chain.params().getWebhookUrl() );
		
		
//		MyTimer t = new MyTimer();
//		t.next( "start");
//		MyClient.postToJson( "https://reflection.trading/hook/get-wallet/0x2703161D6DD37301CEd98ff717795E14427a462B", 
//				Util.toJson( "chainId", 369).toString() ).display();
//
//		t.next( "start");
//		
//		MyClient.postToJson( "https://reflection.trading/hook/get-wallet/0x2703161D6DD37301CEd98ff717795E14427a462B", 
//				Util.toJson( "chainId", 137).toString() ).display();
//
//		t.next( "start");

		
	}
}
