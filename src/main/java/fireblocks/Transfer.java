package fireblocks;

import reflection.Config;

public class Transfer {
	
	public static void main( String[] ar) throws Exception {
		Config.ask("Prod");
		
		double amt = 1;
		String asset = Fireblocks.platformBase;
		int from = Accounts.instance.getId("Owner");
		String to = "0x8Ec701ba0a5b03759c3E954df0a21AB0bB087D79"; // Accounts.instance.getAddress( "Admin1"); // "0x50576E2D58d8605a09fD71c3a36fA8394e43eF16"; // admin2
		String note = "transfer matic";
		
		Fireblocks.transfer(
				from, 
				to, 
				asset, 
				amt, 
				note)
		.waitForHash();
	}
}
