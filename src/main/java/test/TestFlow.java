package test;

import fireblocks.Busd;
import fireblocks.Rusd;
import http.MyHttpClient;
import reflection.Config;
import tw.util.S;

import static fireblocks.Accounts.instance;

public class TestFlow {
	static String prod = "localhost";
	
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");
		
		Busd busd = config.newBusd();
		Rusd rusd = config.rusd();

		busd.mint(
				instance.getId( "Admin1"),
				instance.getAddress("Bob"),
				1);
		
		/*
		server.createContext("/api/reflection-api/get-all-stocks", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/reflection-api/get-stocks-with-prices", exch -> handleGetStocksWithPrices(exch) );
		server.createContext("/api/reflection-api/get-stock-with-price", exch -> handleGetStockWithPrice(exch) );
		server.createContext("/api/reflection-api/order", exch -> handleOrder(exch, false) );
		server.createContext("/api/reflection-api/check-order", exch -> handleOrder(exch, true) );
		server.createContext("/api/reflection-api/positions", exch -> handleReqTokenPositions(exch) );		
		server.createContext("/api/redemptions/redeem", exch -> handleRedeem(exch) );
		 */
		
		MyHttpClient cli = new MyHttpClient(prod, 8383);
		//cli.get("api/redemptions/redeem/" + wallet);
		S.out( cli.readString() );
		

	}
}
