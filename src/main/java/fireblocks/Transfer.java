package fireblocks;

import reflection.Config;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = null;
	
	public static void main( String[] ar) throws Exception {
		Config.ask();

		String wallet = "0xcc8d49d715c045011858e49201a71e057a6425d8";
		String asset = "BUSD_BSC";
		double amt = 19420.54959346867;
		String note = "BUSD to binance.us";
		
		int id = Accounts.instance.getId("Peter Spiro");
		Fireblocks.transfer(id, wallet, asset, amt, note).waitForHash();
	}
	
	public static String transfer(String asset, int srcAccountId, String destAddress, String amount, String note) throws Exception {
		return Fireblocks.transfer( srcAccountId, destAddress, asset, Double.valueOf(amount), note).id();
	}		
}
