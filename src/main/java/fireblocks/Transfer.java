package fireblocks;

import reflection.Config;

public class Transfer {
	
	public static void main( String[] ar) throws Exception {
		Config.ask();
		
		double amt = .1;
		String asset = Fireblocks.platformBase;
		int from = Accounts.instance.getId("Owner");
		String to = Accounts.instance.getAddress( "Admin1"); // "0x50576E2D58d8605a09fD71c3a36fA8394e43eF16"; // admin2
		String note = "test errors";
		
		Fireblocks.transfer(
				from, 
				to, 
				asset, 
				amt, 
				note)
		.waitForHash();
	}
}
