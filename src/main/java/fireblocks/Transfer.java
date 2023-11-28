package fireblocks;

import reflection.Config;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = null;
	
	public static void main( String[] ar) throws Exception {
		Config.ask();

		String wallet = "0xaa50d56e43e1a95a39b357bf0279f218942a1049";
		String asset = "USDC";
		double amt = 100;
		String note = "Send 100 USDC to Qazi";
		
		int id = Accounts.instance.getId("Peter Spiro");
		Fireblocks.transfer(id, wallet, asset, amt, note).waitForHash();
	}
	
	public static String transfer(String asset, int srcAccountId, String destAddress, String amount, String note) throws Exception {
		return Fireblocks.transfer( srcAccountId, destAddress, asset, Double.valueOf(amount), note).id();
	}		
}
