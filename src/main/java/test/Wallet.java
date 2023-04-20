package test;

import fireblocks.Erc20;
import json.MyJsonArray;
import json.MyJsonObject;
import positions.MoralisServer;
import reflection.Config;
import tw.util.S;

public class Wallet {
	//static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String myWallet = "0xb95bf9c71e030fa3d8c0940456972885db60843f";
	

	
	public static void main(String[] args) throws Exception {
		Config config = Config.readFrom("Desktop-config");

		MyJsonArray poss = MoralisServer.reqPositions(myWallet);
		
		for (MyJsonObject pos : poss) {
			double val = Erc20.fromBlockchain( pos.getString("balance"), pos.getInt("decimals") );
			pos.remove("possible_spam");
			pos.put( "val", S.fmt2(val) );
			pos.display();
			S.out();
		}
		
		MoralisServer.reqAllowance(config.newBusd().address(), myWallet, config.newRusd().address() )
			.display();
	}
	
//	public static void main(String[] args) {
//		S.out("busd: " + fromBlockchainHex("145660ddc59b3fc000", 18) );
//		S.out("stock: " + fromBlockchainHex("19e285848b0f0000", 18) );
//	}
	
}

