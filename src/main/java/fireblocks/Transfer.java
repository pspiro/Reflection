package fireblocks;

import reflection.Config;

public class Transfer {
	
	public static void main( String[] ar) throws Exception {
		Config.read();
		
		double amt = .1;
		String asset = Fireblocks.platformBase;
		int from = Accounts.instance.getId("Owner");
		String to = Accounts.instance.getAddress( "Bob"); 
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
