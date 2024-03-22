package fireblocks;

import reflection.Config;
import reflection.Stocks;
import tw.google.NewSheet;
import tw.util.S;

public class MintRusd {
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Dev-config");
		String wallet = "0x2703161D6DD37301CEd98ff717795E14427a462B";
		double amt = 1;
		
		S.out( "Minting %s RUSD for %s in %s", amt, wallet, config.getTabName() );
		mint(wallet, amt, config);
	}
	
	public static void mint(String wallet, double amt, Config config) throws Exception {

		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		config.rusd().mintRusd( wallet, amt, stocks.getAnyStockToken() )
			.waitForCompleted();
	}

	public static void burn(String wallet, double amt, Config config) throws Exception {

		Stocks stocks = new Stocks();
		stocks.readFromSheet( NewSheet.getBook( NewSheet.Reflection), config);
		
		config.rusd().burnRusd( wallet, amt, stocks.getAnyStockToken() )
			.waitForCompleted();
	}
}
