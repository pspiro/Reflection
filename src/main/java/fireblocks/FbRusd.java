package fireblocks;

import common.Util;
import tw.util.S;
import web3.Busd;
import web3.RetVal;
import web3.Rusd.IRusd;
import web3.Stablecoin;
import web3.StockToken;

public class FbRusd extends FbErc20 implements IRusd {
	static final String buyRusdKeccak = "8a854e17";
	static final String sellRusdKeccak = "5690cc4f"; 
	static final String buyStockKeccak = "58e78a85";
	static final String sellStockKeccak = "5948f1f0";
	static final String addOrRemoveKeccak = "89fa2c03";
	static final String swapKeccak = "62835413";

	public FbRusd(String address, int decimals) throws Exception {
		super( address, decimals, "RUSD");
	}
	
	/** Deploy RUSD
	 *  @return deployed address */
	public static String deploy(String filename, String refWallet, String adminAddr) throws Exception {
		S.out( "Deploying RUSD from owner with refWallet %s and admin %s", refWallet, adminAddr);
		String[] paramTypes = { "address", "address" };
		Object[] params = { refWallet, adminAddr };
		
		return deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				paramTypes, 
				params, 
				"Deploy RUSD"
		);
	}

	/** Buy with either RUSD or FBusd; can also be used to burn RUSD
	 * @return id */
	@Override public RetVal buyStock(String adminKey, String userAddr, Stablecoin stablecoin, double stablecoinAmt,
			StockToken stockToken, double stockTokenAmt) throws Exception {
		
		Util.isValidAddress(userAddr);
		
		// allow minting stock tokens in dev only
		// we allow minting as part of a promotion
		//Util.require( stablecoinAmt > 0 || Fireblocks.isDev(), "Cannot buy stock with zero stablecoin");
		
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { 
				userAddr,
				stablecoin.address(), 
				stockToken.address(),
				stablecoin.toBlockchain(stablecoinAmt),
				stockToken.toBlockchain(stockTokenAmt) 
			};
		
		// you should check (a) that approval was done, and (b) that there is
		// sufficient coin in the source wallet. pas
		
		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);		

		S.out( "Account %s buying %s %s with %s %s for user %s",
				adminAcctId, params[4], stockToken.address(), params[3], stablecoin.address(), userAddr);
		return call( adminAcctId, buyStockKeccak, paramTypes, params, stockTokenAmt == 0 ? "RUSD burn" : "RUSD buy stock");
	}
	
	/** Sell stock with either FBusd OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with FBusd)
	 *  
	 *  Also used to mint RUSD */
	@Override public RetVal sellStockForRusd(String adminKey, String userAddr, double rusdAmt, StockToken stockToken,
			double stockTokenAmt) throws Exception {

		Util.isValidAddress(userAddr);

		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				m_address, 
				stockToken.address(), 
				toBlockchain( rusdAmt),
				stockToken.toBlockchain( stockTokenAmt) 
		};
		
		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);		

		S.out( "Account %s selling %s %s for %s RUSD for user %s",
				adminAcctId, 
				params[4], 
				stockToken.address(), 
				params[3], 
				userAddr);
		
		return call( adminAcctId, sellStockKeccak, paramTypes, params, stockTokenAmt == 0 ? "RUSD mint" : "RUSD sell stock");
	}

	/** Burn RUSD from user wallet and transfer FBusd from RefWallet to user wallet
	 *  Since we only pass one amount, RUSD must have same number of decimals as FBusd */
	@Override public RetVal sellRusd(String adminKey, String userAddr, Busd busd, 
			double amt) throws Exception {
		
		Util.isValidAddress(userAddr);

		String[] paramTypes = { "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				busd.address(),
				busd.toBlockchain(amt),
				toBlockchain(amt)
		};
		
		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);

		S.out( "Account %s user %s redeeming %s RUSD for FBusd",
				adminAcctId, userAddr, amt);
		
		return Fireblocks.call2( 
				adminAcctId, 
				m_address, 
				sellRusdKeccak, 
				paramTypes, 
				params, 
				"RUSD sell RUSD");
	}

	/** Not used yet, for testing only */
	RetVal buyRusd(String userAddr, Busd busd, double amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256", "uint256" };
		Object[] params = { 
				userAddr, 
				busd.address(),
				busd.toBlockchain(amt),
				toBlockchain(amt)
		};

		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);		

		S.out( "Account %s user %s buying %s RUSD with FBusd", adminAcctId, userAddr, amt);

		return Fireblocks.call2( 
				adminAcctId, 
				m_address,
				buyRusdKeccak, 
				paramTypes, 
				params, 
				"RUSD buy RUSD"
		);
	}


	/** There is a RUSD.buyRusd method but it is for the future, currently never called. 
	 * @return */
	// public void buyRusd() {
	// }


//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount


	@Override public RetVal addOrRemoveAdmin(String owner, String adminAddr, boolean add) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		Object[] params = { adminAddr, add ? 1 : 0 };
		
		S.out( "Owner adding or removing admin %s (%s)", adminAddr, add);
		
		return call(
				Accounts.instance.getId( owner),
				addOrRemoveKeccak, 
				paramTypes, 
				params, 
				"RUSD add admin");
	}
	
	@Override public RetVal swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { 
				userAddr, 
				stockToBurn.address(),
				stockToMint.address(),
				stockToBurn.toBlockchain(burnAmt),
				stockToMint.toBlockchain(mintAmt)
		};
		
		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);
		S.out( "Account %s is swapping %s of %s for %s of %s for wallet %s",
				adminAcctId, burnAmt, stockToBurn.address(), mintAmt, stockToMint.address(), userAddr);
		
		return call(
				adminAcctId,
				swapKeccak,
				paramTypes,
				params,
				"RUSD swap");
	}

//	/** RUSD has no mint function, so we sell zero shares of stock */
//	public RetVal mintStock(double qty, StockToken stockToken, String address) throws Exception {
//		return buyStockWithRusd( address, 0, stockToken, qty);
//	}

	/** RUSD has no mint function, so we sell zero shares of stock */
	public RetVal mintRusd(String address, double amt, StockToken anyStockToken) throws Exception {
		return sellStockForRusd( null, address, amt, anyStockToken, 0.);
	}

//	@Override public RetVal approve(String ownerKey, String spender, double amt) throws Exception {
//		throw new Exception( "not implemented");
//	}

	/** RUSD has no mint function, so we sell zero shares of stock */
//	public RetVal burnRusd(String address, double amt, StockToken anyStockToken) throws Exception {
//		return buyStockWithRusd( null, address, amt, anyStockToken, 0);
//	}
}

// we are storing address, dec, name redundantly with Rusd and Busd. pas