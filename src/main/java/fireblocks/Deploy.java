package fireblocks;

import json.MyJsonObject;
import positions.MoralisServer;
import reflection.RefCode;
import reflection.RefException;
import tw.util.IStream;
import tw.util.S;

public class Deploy {

	public static void main(String[] args) throws Exception {
		//Tab tab = NewSheet.getTab(NewSheet.Reflection, "Prod-symbols");
		//for (ListEntry row : tab.fetchRows() ) {
		
		Fireblocks.setProdVals();
		deploy("c:/work/smart-contracts/test.bytecode",
				4,
				new String[] { "string", "string" },
				new String[] { "peter", "spiro" },
				"deploy test contract");
	}

	/** The wallet associated w/ ownerAcctId becomes the owner of the deployed contract.
	 *  The parameters passed here are the passed to the constructor of the smart contract
	 *  being deployed. The whole thing takes 30 seconds.
	 *  @return the deployed contract address */
	public static String deploy(String filename, int ownerAcctId, String[] paramTypes, Object[] params, String note) throws Exception {
		S.out( "Deploying contract");
		String data = new IStream(filename).readln();
		String id = Fireblocks.call( ownerAcctId, "0x0", data, paramTypes, params, note);
		
		// if there's an error, you got message and code
		
		//{"message":"Source is invalid","code":1427}		
		
		// it takes 30 seconds to deploy a contract and get the contract address back; how long does it take from javascript?
		S.out( "  fireblocks id is %s", id);

		S.out( "  waiting for blockchain transaction hash");
		String txHash = Fireblocks.getTransHash( id, 60);
		S.out( "  blockchain transaction hash is %s", txHash);

		S.out( "  waiting for deployed address");
		return getDeployedAddress(txHash);
	}
	

	/** Query the blockchain transaction through Moralis until the transaction
	 *  is there AND it contains the receipt_contract_address field;
	 *  takes about 17 seconds. */
	static String getDeployedAddress(String txHash) throws Exception {
		for (int i = 0; i < 3*60; i++) {
			if (i > 0) S.sleep(1000);
			
			S.out( "    querying...");
			MyJsonObject obj = MoralisServer.queryTransaction(txHash,  "goerli");
			String addr = obj.getString("receipt_contract_address");
			if (S.isNotNull(addr) ) {
				return addr;
			}
		}
		throw new RefException( RefCode.UNKNOWN, "Could not get blockchain transaction");		
	}
}
