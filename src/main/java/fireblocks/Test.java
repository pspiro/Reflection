package fireblocks;

import json.MyJsonObject;
import positions.MoralisServer;
import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

public class Test {
	static String testAddr = "0x14D7E955B156D9F294DAeA57b7b119dB54D71124";
	
	static String getfirstKeccak = "0xaf3e9eb6";  // no 0x??????????????????????
	
//	public static void main(String[] args) throws ParseException {
//		MoralisServer.queryTransaction("0x675cba1e5f2cc316674c297363e9be69f6d34625705be098c14c2d9a0be0ba92", "goerli").display(); 
//	}
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		
		MyJsonObject obj = Fireblocks.deploy( "c:/temp/bytecode.t", 
				new String[] { "string", "string" },
				new Object[] { "first", "last" },
				"deploy test first last");
		obj.display();
		
		String id = obj.getString("id");  // it takes 30 seconds to deploy a contract and get the contract address back; how long does it take from javascript?
		
		String txHash = getTransHash( id);

		String addr = getDeployedAddress(txHash);
		S.out( addr);
	}

	/** Query the blockchain transaction through Moralis until the transaction
	 *  is there AND it contains the receipt_contract_address field;
	 *  takes about 17 seconds. */
	static String getDeployedAddress(String txHash) throws Exception {
		for (int i = 0; i < 3*60; i++) {
			if (i > 0) S.sleep(1000);
			
			MyJsonObject obj = MoralisServer.queryTransaction(txHash,  "goerli");
			String addr = obj.getString("receipt_contract_address");
			if (S.isNotNull(addr) ) {
				return addr;
			}
			S.out( "not yet");
			obj.display();
		}
		throw new RefException( RefCode.UNKNOWN, "Could not get blockchain transaction");		
	}
	
	/** Query the transaction from Fireblocks until it contains the txHash value;
	 *  takes about 13 seconds. */
	static String getTransHash(String fireblocksId) throws Exception {
		for (int i = 0; i < 5*60; i++) {
			if (i > 0) S.sleep(1000);
			MyJsonObject trans = Fireblocks.getTransaction( fireblocksId);
			S.out( "status: %s  hash: %s", trans.getString("status"), trans.getString("txHash") );
			
			String txHash = trans.getString("txHash");
			if (S.isNotNull( txHash) ) {
				return txHash;
			}
			
			String status = trans.getString("status");
			if (status.equals( "COMPLETED") ) {
				break;
			}
		}
		
		throw new RefException( RefCode.UNKNOWN, "Could not get transaction hash");
	}
}

