package fireblocks;

import common.Util;
import tw.util.S;

public class Rusd extends Erc20 {
	
	// BUSD on binance and ethereum has 18 decimals
	// USDP on ethereum has 18 decimals
	// USDC and USDT on ethereum have 6 decimals
	// RUSD must match the number of decimals of the non-RUSD stablecoin because
	//   buyRusd and sellRusd takes only one number
	// stock tokens have 18 decimals
	
	
	// keccaks
	static final String buyRusdKeccak = "8a854e17";
	static final String sellRusdKeccak = "5690cc4f"; 
	static final String buyStockKeccak = "58e78a85";
	static final String sellStockKeccak = "5948f1f0";
	public static final String addOrRemoveKeccak = "89fa2c03";
	static final String swapKeccak = "62835413";

	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	
//	String rusdAddr = "0x31ed1e80db8a6e82b2f73c4cb37a1390fe7793a7"; // deploy( "c:/work/bytecode/rusd.bytecode");
//	String ibmAddr = "0xfdaf3b9c6665fe47eb701abea7429d0c1b5d30a1"; // StockToken.deploy( "c:/work/bytecode/stocktoken.bytecode", "IBM", "IBM", rusdAddr);
	
	/** NOTE: rusdDecimals must match the number of decimals from the source
	 *  code; the value here is not passed into the smart contract constructor 
	 * @throws Exception */
	public Rusd( String rusdAddr, int rusdDecimals) throws Exception {
		super( rusdAddr, rusdDecimals, "RUSD");
		Util.require( rusdDecimals == 6, "Wrong number of decimals for RUSD " + rusdDecimals);
	}
	
	/** Deploy RUSD
	 *  @return deployed address */
	public void deploy(String filename, String refWallet, String adminAddr) throws Exception {
		S.out( "Deploying RUSD from owner with refWallet %s and admin %s", refWallet, adminAddr);
		String[] paramTypes = { "address", "address" };
		Object[] params = { refWallet, adminAddr };
		
		m_address = deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				paramTypes, 
				params, 
				"Deploy RUSD"
		);
	}
	
	
	

	/** Buying stock with either BUSD OR RUSD; need to test it both ways.
	 * 
	 *  IMPORTANT, READ THIS FOR FOR TROUBLE-SHOOTING
	 *  
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD)
	 *  and you must have enough base coin in the refWallet */
	public String buyStockWithRusd(String userAddr, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return buyStock( 
				userAddr,
				this,
				stablecoinAmt,
				stockToken,
				stockTokenAmt
		);
	}
	
	/** Buy with either RUSD or BUSD
	 * @return id */
	public String buyStock(String userAddr, Erc20 stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
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
		return call( adminAcctId, buyStockKeccak, paramTypes, params, "RUSD buy stock");
	}
	
	/** Sell stock with either BUSD OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD) */
	public String sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
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
		
		return call( adminAcctId, sellStockKeccak, paramTypes, params, "RUSD sell stock");
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

		S.out( "Account %s user %s buying %s RUSD with BUSD", adminAcctId, userAddr, amt);

		return Fireblocks.call2( 
				adminAcctId, 
				m_address,
				buyRusdKeccak, 
				paramTypes, 
				params, 
				"RUSD buy RUSD"
		);
	}

	/** Burn RUSD from user wallet and transfer BUSD from RefWallet to user wallet
	 *  Since we only pass one amount, RUSD must have same number of decimals as BUSD */
	public RetVal sellRusd(String userAddr, Busd busd, double amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				busd.address(),
				busd.toBlockchain(amt),
				toBlockchain(amt)
		};
		
		int adminAcctId = Accounts.instance.getAdminAccountId(userAddr);		

		S.out( "Account %s user %s selling %s RUSD for BUSD",
				adminAcctId, userAddr, amt);
		
		return Fireblocks.call2( 
				adminAcctId, 
				m_address, 
				sellRusdKeccak, 
				paramTypes, 
				params, 
				"RUSD sell RUSD");
	}

	/** There is a RUSD.buyRusd method but it is for the future, currently never called. */
	// public void buyRusd() {
	// }


//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount


	public void addOrRemoveAdmin(String adminAddr, boolean add) throws Exception {
		Util.require( add, "remove not supported yet for safety");
		
		String[] paramTypes = { "address", "uint256" };
		Object[] params = { adminAddr, 1 };
		
		S.out( "Owner adding or removing admin %s (%s)", adminAddr, add);
		
		call(	Accounts.instance.getId( "Owner"),
				addOrRemoveKeccak, 
				paramTypes, 
				params, 
				"RUSD add admin");
	}
	
	public String swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
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

	/** RUSD has no mint function, so we sell zero shares of stock */
	public String mint(String address, double amt, StockToken anyStockToken) throws Exception {
		return sellStockForRusd( address, amt, anyStockToken, 0);
	}
}
