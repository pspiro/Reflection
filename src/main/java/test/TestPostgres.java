package test;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	public static void main(String[] args) throws Exception {
		Config config = Config.ask();
		
		config.rusd().showAllTransactions();
		S.out();
		config.rusd().showBalances();

		
//		config.readStocks().getStock("AAPL").getToken().showAllTransactions();
//		S.out();
//		config.readStocks().getStock("AAPL").getToken().showBalances();
	}
	
	
}
