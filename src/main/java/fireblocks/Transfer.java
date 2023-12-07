package fireblocks;

import common.Util;
import reflection.Config;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = null;
	
	public static void main( String[] ar) throws Exception {
		Config.ask();
		
		String wallet = Util.input( "Enter wallet:");
		String note = Util.input( "Enter note:");
		String asset = "USDC";
		double amt = 0;
		
		int id = Accounts.instance.getId("Peter Spiro");
		Fireblocks.transfer(id, wallet, asset, amt, note).waitForHash();
	}
	
	public static String transfer(String asset, int srcAccountId, String destAddress, String amount, String note) throws Exception {
		return Fireblocks.transfer( srcAccountId, destAddress, asset, Double.valueOf(amount), note).id();
	}		
}
