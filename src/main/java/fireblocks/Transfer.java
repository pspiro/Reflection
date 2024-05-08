package fireblocks;

import reflection.Config;

public class Transfer {
	
	public static void main( String[] ar) throws Exception {
		Config.ask("Prod");
		
		double amt = .1;
		String asset = Fireblocks.platformBase;
		int from = Accounts.instance.getId("Owner");
		// String to = "0x8Ec701ba0a5b03759c3E954df0a21AB0bB087D79";  // Refblocks owner
		// String to = "0x6117A8a8df7db51662e9555080Ab8DeF0E11c4d3";  // Refblocks admin1
		String to = "0x6c3644344E5AdDFb39144b1B76Cae79da6719228"; 
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
