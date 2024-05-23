package test;

import reflection.Config;
import reflection.Stocks;
import testcase.Cookie;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	
	public static void main(String[] args) throws Exception {
		Config c = Config.ask( "Dt");
		Stocks stocks = c.readStocks();
		
		S.out( c.rusd().getPosition( Cookie.wallet) );
		S.out( stocks.getStockByConid(265598).getToken().getPosition( Cookie.wallet) );
		
		c.rusd().buyStockWithRusd(Cookie.wallet, 10, stocks.getStockByConid(265598).getToken(), 3)
				.waitForHash();

		S.out( c.rusd().getPosition( Cookie.wallet) );
		S.out( stocks.getStockByConid(265598).getToken().getPosition( Cookie.wallet) );
		
//		c.matic().transfer( c.ownerKey(), c.refWalletAddr(), .007)
//			.displayHash();
//		
//		c.busd().approve( c.refWalletKey(), c.rusdAddr(), 1000000)
//			.displayHash();
		
//		S.out( c.busd().getAllowance( c.refWalletAddr(), c.rusdAddr() ) );
	}
}
