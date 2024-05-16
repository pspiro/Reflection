package refblocks;

import org.web3j.tx.TransactionManager;

import common.Util;
import tw.util.S;
import web3.Busd;
import web3.Erc20;
import web3.RetVal;
import web3.Rusd.IRusd;
import web3.Stablecoin;
import web3.StockToken;

/** Implements the Rusd contract methods that are writable */
public class RbRusd extends Erc20 implements IRusd {
	static String rpc = "https://polygon-rpc.com/";
	static long gas = 40000; // make this big enough for all transactions
	
	public RbRusd(String address, int decimals) {
		super( address, decimals, "RUSD");
	}

	/** deploy Rusd */
	public static String deploy( String ownerKey, String refWallet, String admin1) throws Exception {
		return Rusd.deploy( 
				Refblocks.web3j,
				Refblocks.getWaitingTm( ownerKey),
				Refblocks.getGp( Refblocks.deployGas),
				refWallet, admin1
				).send().getContractAddress();
	}

	/** load generated Rusd that we can use to call smart contract methods that write to the blockchain
	 *  note that we no longer need to have tm passed in because we can get it from Refblocks */
	public Rusd load(TransactionManager tm) throws Exception {
		return Rusd.load( 
				address(), 
				Refblocks.web3j, 
				tm, 
				Refblocks.getGp( 1000000)  // this is good for everything except deployment
				);
	}

	@Override public RetVal buyStock( String adminKey, String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken,
			double stockTokenAmt) throws Exception {
		
		Util.reqValidKey(adminKey);
		Util.reqValidAddress(userAddr);
		
		S.out( "%s buying %s %s for %s %s", 
				Refblocks.getAddressPk( adminKey),
				stockTokenAmt,
				stockToken.name(),
				stablecoinAmt,
				stablecoin.name() );
		
		return Refblocks.exec( adminKey, tm -> load( tm).buyStock(
				userAddr, 
				stablecoin.address(), 
				stockToken.address(), 
				stablecoin.toBlockchain( stablecoinAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				) );
	}

	@Override public RetVal sellStockForRusd( String adminKey, String userAddr, double rusdAmt, StockToken stockToken, double stockTokenAmt)
			throws Exception {

		Util.reqValidKey(adminKey);
		Util.reqValidAddress(userAddr);

		S.out( "%s sell %s %s for %s RUSD",
				Refblocks.getAddressPk( adminKey),
				stockTokenAmt,
				stockToken.name(),
				rusdAmt);

		return Refblocks.exec( adminKey, tm -> load( tm).sellStock( 
				userAddr,
				address(), 
				stockToken.address(), 
				toBlockchain( rusdAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				) );
	}

	@Override public RetVal sellRusd( String adminKey, String userAddr, Busd busd, double amt) 
			throws Exception {
		
		Util.reqValidKey(adminKey);
		Util.reqValidAddress(userAddr);

		S.out( "%s redeeming %s RUSD for %s %s",
				Refblocks.getAddressPk( adminKey),
				amt,
				amt,
				busd.name() );

		return Refblocks.exec( adminKey, tm -> load( tm).sellRusd(
				userAddr,
				busd.address(),
				busd.toBlockchain( amt),
				toBlockchain( amt)
				) );
	}

	@Override public RetVal addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception {
		Util.reqValidKey(ownerKey);
		Util.reqValidAddress(address);

		S.out( "%s %s admin %s",
				Refblocks.getAddressPk( ownerKey),
				add ? "adding" : "removing",
				address);
				
		return Refblocks.exec( ownerKey, tm -> load( tm).addOrRemoveAdmin( 
				address, 
				add) );
	}

}
