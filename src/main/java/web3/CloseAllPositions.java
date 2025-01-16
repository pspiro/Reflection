package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;
import common.Util;
import http.MyClient;
import tw.util.S;

public class CloseAllPositions {
	static Chain chain;
	
	public static void main(String[] args) throws Exception {
		// don't destroy 
//		0xf4cd0048fcee821ba1419dc7c997128377abf386 
//		0x19758c2ab3f83586d61c40f95550a4a15737e589 
//		0xa8a4cb767a9a29240ac684acca5f8ce363074a1e 
//		0x8d5676ddd6071359a8e9bd18185aa03b5e05080e 
		
		
		try (MyScanner s = new MyScanner() ) {
			String name = s.getString( "enter chain name: [Polygon]", "Polygon");
			
			chain = Chains.readOne( name, true);
			
			for (var stock : chain.getTokensList() ) {
				close( stock);
			}
		}
	}

	private static void close(StockToken token) throws Exception {
		double supply = token.queryTotalSupply();
		
		if (supply >= .0001) {
			
			double price = MyClient.getJson("https://reflection.trading/api/get-price/" + token.conid() ).getDouble( "bid");
						
			var map = token.getAllBalances();  // map address -> balance
			
			Util.forEach( map, (wallet,qty) -> {
				if (qty > .0001) {
					double qty2 = Util.truncate( qty, 4);
					
					double amt = qty2 * price;

					S.out( "SELL stock=%s  supply=%s  price=%s  wallet=%s  qty=%s  amt=%s",
							token.name(), supply, price, wallet, qty2, S.fmt4( amt) );

					
					Util.wrap( () -> {
						chain.rusd().preSellStock(wallet, amt, token, qty2);
						chain.rusd().sellStockForRusd(wallet, amt, token, qty2).waitForReceipt();
					});
				}
			});
			
		}
	
	}
}