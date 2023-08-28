package fireblocks;

import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;

public class MintRusd {
	public static void main(String[] args) throws Exception {
		mint("0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce", 100000);
	}
	
	static void mint(String wallet, double amt) throws Exception {
		Config config = Config.readFrom("Dt-config");
		
		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		String id = config
				.rusd()
				.sellStockForRusd( wallet, amt, stocks.getAnyStockToken(), 0);
		Fireblocks.waitForStatus(id, "CONFIRMED");
		
		
	}
}
