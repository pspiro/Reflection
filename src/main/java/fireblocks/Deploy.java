package fireblocks;

import json.MyJsonObject;
import positions.MoralisServer;
import reflection.Config;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.IStream;
import tw.util.S;
import static fireblocks.Accounts.instance;

public class Deploy {
	// it seems that you have to wait or call w/ the same Admin
	// if you need the first transaction to finish because
	// Fireblocks checks and thinks it will fail if the first
	// one is not done yet
	
	// deploy RUSD and all stock tokens
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");
		
		Rusd rusd = config.newRusd();
		Busd busd = config.newBusd();
		
		Util.require( S.isNotNull( busd.address() ), "BUSD address is missing");
		Util.require( S.isNull( rusd.address() ), "RUSD is already deployed");
		
		instance.read();
		
		// deploy RUSD
		rusd.deploy( 
				"c:/work/bytecode/rusd.bytecode",
				instance.getAddress( "RefWallet"),
				instance.getAddress( "Admin1")
		);
		
		// update spreadsheet with deployed address
		config.setRusdAddress( rusd.address() );
		
		// let RefWallet approve RUSD to transfer BUSD
		busd.approve( 
				instance.getId( "RefWallet"), // called by
				rusd.address(), // approving
				1000000000  // $1B
		);
		
		// add a second owner
		rusd.addOrRemoveAdmin(
				instance.getId( "Owner"), 
				instance.getAddress( "Admin2"), 
				true
		);

		// deploy stock tokens that are active and have an empty token address
		for (ListEntry row : NewSheet.getTab( NewSheet.Reflection, config.symbolsTab() ).fetchRows(false) ) {
			if ("Y".equals( row.getValue( "Active") ) && S.isNull( row.getValue( "Token Address") ) ) {
				// deploy stock token
				StockToken token = StockToken.deploy( 
						"c:/work/bytecode/stocktoken.bytecode", 
						row.getValue( "Contract Name"),
						row.getValue( "Contract Symbol"),
						rusd.address()
				);
				
				// update row on Symbols tab with new stock token address
				row.setValue( "Token Address", token.address() );
				row.update();
			}
		}
	}

	// move this into Erc20? Or let it return a

	/** The wallet associated w/ ownerAcctId becomes the owner of the deployed contract.
	 *  The parameters passed here are the passed to the constructor of the smart contract
	 *  being deployed. The whole thing takes 30 seconds.
	 *  @return the deployed contract address */
	public static String deploy(String filename, int ownerAcctId, String[] paramTypes, Object[] params, String note) throws Exception {
		S.out( "Deploying contract from %s", filename);
		
		// very strange, sometimes we get just the bytecode, sometimes we get a json object
//		String bytecode = MyJsonObject.parse( new IStream(filename).readAll() )
//				.getString("object");
		String bytecode = new IStream(filename).readln();
		
		String id = Fireblocks.call( ownerAcctId, "0x0", bytecode, paramTypes, params, note);
		
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
			MyJsonObject obj = MoralisServer.queryTransaction(txHash, Fireblocks.moralisPlatform);
			String addr = obj.getString("receipt_contract_address");
			if (S.isNotNull(addr) ) {
				S.out( "contract deployed to " + addr);
				return addr;
			}
		}
		throw new RefException( RefCode.UNKNOWN, "Could not get blockchain transaction");		
	}
	
}
