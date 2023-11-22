package fireblocks;

import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;

public class MintRusd {
	public static void main(String[] args) throws Exception {
		String wallet = "0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce";
		double amt = 2000;
		mint(wallet, amt);
	}
	
	public static void mint(String wallet, double amt) throws Exception {
		Config config = Config.ask();
		
		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		config.rusd().mint( wallet, amt, stocks.getAnyStockToken() )
			.waitForStatus("COMPLETED");
		
		
	}
}
