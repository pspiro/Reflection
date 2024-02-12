package fireblocks;

import common.Util;
import reflection.Config;

public class MintStockToken {
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		double amt = Double.parseDouble( Util.ask( "Enter amount:") );
		String tok = Util.ask("Enter token name");
		String wallet = Util.ask( "Enter destination wallet:");
		
		StockToken st = config.readStocks().getStockBySymbol(tok).getToken();
		
		if (Util.confirm( null, String.format("You will mint %s %s for %s", amt, tok, wallet) ) ) {
			config.rusd().buyStockWithRusd(wallet, 0, st, amt).waitForHash();
		}	
	}
}
