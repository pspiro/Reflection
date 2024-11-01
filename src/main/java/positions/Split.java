package positions;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.SingleChainConfig;
import tw.util.S;
import web3.StockToken;

/** BE CAREFUL. If it is a 10 for 1 split, you need to mint 9x current position */

/** it would be better to do it right from the holder panel */
public class Split {
	final static String stockAddr = "0x51cca1091a01d2d002d2e9dab6a2c45a24fcf6e8";
	
	public static void main(String[] args) throws Exception {
		SingleChainConfig c = SingleChainConfig.ask();
		
		StockToken stock = c.chain().getTokenByAddress(stockAddr);
		
		JsonArray ar = new JsonArray();
		
		ar.add( Util.toJson( "wallet", "0x2703161d6dd37301ced98ff717795e14427a462b", "amt", 0.0747) );
		ar.add( Util.toJson( "wallet", "0x037cad8067ec11cc78de10a8b5bcbcd5ebef259b", "amt", 0.4171) );
		ar.add( Util.toJson( "wallet", "0xf1a43101fdded0f7b71e88c1d55dd107b2915740", "amt", 0.063) );
		ar.add( Util.toJson( "wallet", "0x713d09c86a171e737a1f1dd5d8141dfb3a680def", "amt", 0.3867) );
		ar.add( Util.toJson( "wallet", "0xabb26bcdfb5574f28010a9bac8ddd2de528547c7", "amt", 0.4111) );
		
		/** BE CAREFUL. If it is a 10 for 1 split, you need to mint 9x current position */
		
		for (JsonObject user : ar) {
			String wallet = user.getString("wallet");
			double amt = user.getDouble("amt") * 8;
			
			S.out( "minting %s for %s", amt, wallet);
			
			c.rusd().mintStockToken( wallet, stock, amt).waitForReceipt();
			S.out( "check one");
		}
	}
}
