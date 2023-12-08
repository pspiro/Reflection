package fireblocks;

import common.Util;
import reflection.Config;
import tw.util.S;

public class MintStockToken {
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		double amt = Double.parseDouble( Util.ask( "Enter amount:") );
		String tok = Util.ask("Enter FULL token name");
		String wallet = Util.ask( "Enter destination wallet:");
		
		StockToken st = config.readStocks().getStock(tok).getToken();
		
		if (S.confirm( null, String.format("You will mint %s %s for %s", amt, tok, wallet) ) ) {
			config.rusd().buyStock(wallet, config.rusd(), 0, st, amt).waitForHash();
		}	
	}
}
