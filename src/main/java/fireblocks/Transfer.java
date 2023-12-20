package fireblocks;

import reflection.Config;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = null;
	
	public static void main( String[] ar) throws Exception {
		Config.ask();
		
		double amt = 1;
		String asset = "MATIC_POLYGON";
		int from = Accounts.instance.getId("Admin1");
		String to = "0x50576E2D58d8605a09fD71c3a36fA8394e43eF16"; // admin2 
		String note = "test errors";
		
		Fireblocks.transfer(from, to, asset, amt, note).waitForHash();
	}
	
	public static String transfer(String asset, int srcAccountId, String destAddress, String amount, String note) throws Exception {
		return Fireblocks.transfer( srcAccountId, destAddress, asset, Double.valueOf(amount), note).id();
	}		
}
