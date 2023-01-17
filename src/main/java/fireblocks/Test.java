package fireblocks;

import json.MyJsonObject;
import positions.MoralisServer;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

public class Test {
	static String testAddr = "0x14D7E955B156D9F294DAeA57b7b119dB54D71124";
	
	static String getfirstKeccak = "0xaf3e9eb6";  // no 0x??????????????????????
	
	public static void main(String[] args)  {
		S.out( System.currentTimeMillis() );
		//MoralisServer.queryTransaction("0x675cba1e5f2cc316674c297363e9be69f6d34625705be098c14c2d9a0be0ba92", "goerli").display(); 
	}
	
//	public static void main(String[] args) throws Exception {
//		Fireblocks.setVals();
//		
//		MyJsonObject obj = Deploy.deploy( "c:/temp/bytecode.t", 
//				new String[] { "string", "string" },
//				new Object[] { "first", "last" },
//				"deploy test first last");
//		obj.display();
//		
//		String id = obj.getString("id");  // it takes 30 seconds to deploy a contract and get the contract address back; how long does it take from javascript?
//		
//		String txHash = getTransHash( id);
//
//		String addr = getDeployedAddress(txHash);
//		S.out( addr);
//	}
//
//
}

