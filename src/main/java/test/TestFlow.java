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
		
		Busd busd = config.busd();
		Rusd rusd = config.rusd();

		busd.mint(
				instance.getId( "Admin1"),
				instance.getAddress("Bob"),
				1);
		
		
		MyHttpClient cli = new MyHttpClient(prod, 8383);
		//cli.get("api/redemptions/redeem/" + wallet);
		S.out( cli.readString() );
		

	}
}
