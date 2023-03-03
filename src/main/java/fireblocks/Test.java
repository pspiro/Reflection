package fireblocks;

import json.MyJsonObject;
import positions.MoralisServer;
import reflection.Config;
import reflection.RefCode;
import reflection.RefException;
import tw.util.IStream;
import tw.util.S;

public class Test {
	// it seems that you have to wait or call w/ the same Admin
	// if you need the first transaction to finish because
	// Fireblocks checks and thinks it will fail if the first
	// one is not done yet
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setTestVals();
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");
		
		Rusd rusd = config.newRusd();
		Busd busd = config.newBusd();
		
		Accounts accounts = Accounts.instance;
		accounts.read();
		
		// deploy StockToken
		StockToken stock = new StockToken("0x2c880f5124c303948d5cc528975180f224110e09");
		
		// mint 100 BUSD
		String id = busd.mint( accounts.getId( "Bob"), accounts.getAddress( "Bob"), 100);
//		busd.mint( accounts.getId( "Admin1"), accounts.getAddress( "Sam"), 100);
		Fireblocks.getTransHash(id, 60);
		
		// let Bob approve RUSD as spender of BUSD
		String id2 = busd.approve( 
				accounts.getId( "Bob"), // called by
				rusd.address(), // approving
				2
		);
		Fireblocks.getTransHash(id2, 60);

		// buy 2 stock with 100 BUSD
		rusd.buyStockWithStablecoin(
				accounts.getId( "Admin1"),
				accounts.getAddress( "Bob"),
				busd.address(),
				busd.decimals(),        // << this faile even though I have 100, why???
				2,						// it works with zero, though; see if you really have 100, or if you have .000100
				stock.address(),
				2
		);
		
		// sell stock for 100 RUSD
		rusd.sellStockForRusd(
				accounts.getId( "Admin1"),
				accounts.getAddress( "Bob"),
				100,  				// RUSD amt
				stock.address(), 	// stock token
				2					// stock token amt
		);

		// buy stock token for 50 RUSD
		rusd.buyStockWithRusd(
				accounts.getId( "Admin1"),
				accounts.getAddress( "Bob"),
				50,
				stock.address(),
				1
		);
		
		// let RefWallet approve RUSD as spender of BUSD
		busd.approve( 
				accounts.getId( "RefWallet"), // called by
				rusd.address(), // approving
				100000
		);

		// sell 50 RUSD for 80 BUSD
		rusd.sellRusd(
				accounts.getId( "Admin1"),
				accounts.getAddress( "Bob"),
				busd.address(),
				50
		);
		
		// buy 2 stock for sam
		// add a second owner
		rusd.addOrRemoveAdmin(
				accounts.getId( "Owner"), 
				accounts.getAddress( "Admin2"), 
				true
		);

		rusd.buyStockWithStablecoin(
				accounts.getId( "Admin2"),
				accounts.getAddress( "Sam"),
				busd.address(),
				busd.decimals(),
				100,
				stock.address(),
				2
		);
		
		
		
		
		// 1 stock + 50 RUSD = 100
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
