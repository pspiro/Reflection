package reflection;

import fireblocks.Fireblocks;
import http.SimpleTransaction;
import http.SimpleTransaction.MyHttpHandler;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.S;

public class Mint implements MyHttpHandler {
	public static void main(String[] args) throws Exception {
		Fireblocks.setTestVals();		
		new Mint().run();
	}
	
	GTable tab;
	
	void run() throws Exception {
		S.out( "Connecting to google sheet/minted tab");
		tab = new GTable( NewSheet.Reflection, "Minted", "Wallet", "Minted");
		
		S.out( "Listening on localhost:8484");
		SimpleTransaction.listen("0.0.0.0", 8484, this);
	}

	@Override public void handle(SimpleTransaction trans) {
		try {
			String wallet = trans.getMap().get( "wallet");
			
			if ("Y".equals( tab.get( wallet.toLowerCase() ) ) ) { 
				throw new Exception("Sorry, this wallet address has already been used. You can create a new wallet and try again.");
			}
			tab.put( wallet.toLowerCase(), "Y");
			
			//Fireblocks.transfer( "0"
			
			
		} 
		catch (Exception e) {
			trans.respond( e.getMessage() );
			e.printStackTrace();
		}
		
	}
}
