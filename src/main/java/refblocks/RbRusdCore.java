package refblocks;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import common.Util;
import fireblocks.RetVal;
import web3.Busd;
import web3.MyCoreBase;
import web3.Rusd.IRusd;
import web3.Stablecoin;
import web3.StockToken;

public class RbRusdCore extends MyCoreBase implements IRusd {
	static String rpc = "https://polygon-rpc.com/";
	static long gas = 40000; // make this big enough for all transactions
	
	public RbRusdCore(String address, int decimals) {
		super( address, decimals, "RUSD");
	}

	public static String deploy( String ownerKey, String refWallet, String admin1) throws Exception {
		return Rusd.deploy( 
				Refblocks.web3j,
				Refblocks.getTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas),
				refWallet, admin1
				).send().getContractAddress();
	}

	public Rusd loadRusd(String privateKey) throws Exception {
		return Rusd.load( 
				address(), 
				Refblocks.web3j, 
				Refblocks.getTm( privateKey ), 
				Refblocks.getGp( 1000000)
				);
	}

	@Override public RetVal buyStock( String adminKey, String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken,
			double stockTokenAmt) throws Exception {
		
		Util.isValidKey(adminKey);
		Util.isValidAddress(userAddr);

		TransactionReceipt rec = loadRusd( adminKey).buyStock( 
				userAddr, 
				stablecoin.address(), 
				stockToken.address(), 
				stablecoin.toBlockchain( stablecoinAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				).send();  // this is going to block which is not what we want
		
		Refblocks.showReceipt( rec);
		
		return null;
	}

	@Override public RetVal sellStockForRusd( String adminKey, String userAddr, double rusdAmt, StockToken stockToken, double stockTokenAmt)
			throws Exception {

		Util.isValidKey(adminKey);
		Util.isValidAddress(userAddr);

		loadRusd( adminKey).sellStock(
				userAddr,
				address(), 
				stockToken.address(), 
				toBlockchain( rusdAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				).send();
		return null;
	}

	@Override public RetVal sellRusd( String adminKey, String userAddr, Busd busd, double amt) 
			throws Exception {
		
		Util.isValidKey(adminKey);
		Util.isValidAddress(userAddr);

		loadRusd( adminKey).sellRusd(
				userAddr,
				busd.address(),
				busd.toBlockchain( amt),
				toBlockchain( amt)
				).send();
		return null;
	}

}
