package fireblocks;

import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;

public class MintRusd {
	public static void main(String[] args) throws Exception {
		mint("0xb781012C022FcD149645e2b16Dfd2E920754514f", 100000);
	}
	
	public static void mint(String wallet, double amt) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		config.rusd().mint( wallet, amt, stocks.getAnyStockToken() )
			.waitForStatus("COMPLETED");
		
		
	}
}
