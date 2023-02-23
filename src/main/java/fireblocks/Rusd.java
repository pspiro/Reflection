package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Rusd {
	
	// busd on binance and ethereum has 18 decimals
	// usdc and usdt on ethereum have 6 decimals
	// stock tokens have 18 decimals
	
	
	// keccaks
	static final String approveKeccak = "095ea7b3";  // call this on BUSD
	static final String buyRusdKeccak = "28c4ef43"; // this can't be right, we never buy rusd. pas // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static final String buyStockKeccak = "58e78a85";
	static final String sellStockKeccak = "5948f1f0";
	static final String mintKeccak = "40c10f19";
	static final String burnKeccak = "9dc29fac";

	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	static final BigDecimal ten = new BigDecimal(10);
	
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setProdValsPolygon();

		String rusdAddr = "0x31ed1e80db8a6e82b2f73c4cb37a1390fe7793a7"; // deploy( "c:/work/bytecode/rusd.bytecode");
		String ibmAddr = "0xfdaf3b9c6665fe47eb701abea7429d0c1b5d30a1"; // StockToken.deploy( "c:/work/bytecode/stocktoken.bytecode", "IBM", "IBM", rusdAddr);
		
		Util.execute( () -> {
			try {
				buyStock( Fireblocks.refWalletAcctId1, rusdAddr, myWallet, rusdAddr, 0, ibmAddr, 1, "buy IBM1");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		Util.execute( () -> {
			try {
				buyStock( Fireblocks.refWalletAcctId2, rusdAddr, myWallet, rusdAddr, 0, ibmAddr, 1, "buy IBM2");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}
	
	/** Deploy RUSD
	 *  @return deployed address */
	static String deploy(String filename) throws Exception {
		S.out( "Deploying RUSD from owner %d with refWallet %s", Fireblocks.ownerAcctId, Fireblocks.refWalletAddr1);
		String[] paramTypes = { "address", "address" };
		Object[] params = { Fireblocks.refWalletAddr1, Fireblocks.refWalletAddr2 };

		return Deploy.deploy( filename, Fireblocks.ownerAcctId, paramTypes, params, "Deploy RUSD");
	}
	

	/** Buying stock with either BUSD OR RUSD; need to test it both ways.
	 * 
	 *  IMPORTANT, READ THIS FOR FOR TROUBLE-SHOOTING
	 *  
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD)
	 *  and you must have enough base coin in the refWallet */
	public static String buyStock(int refWalletAcctId, String rusdAddr, String userAddr, String stablecoinAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt, String note) throws Exception {
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { 
				userAddr,
				stablecoinAddr, 
				stockTokenAddr, 
				toStablecoin( stablecoinAddr, stablecoinAmt), 
				toStockToken( stockTokenAmt) 
			};
		
		// you should check (a) that approval was done, and (b) that there is
		// sufficient coin in the source wallet. pas
		
		S.out( "Refwallet buying %s %s with %s %s for user %s", 
				params[4], stockTokenAddr, params[3], stablecoinAddr, userAddr);
		return Fireblocks.call( refWalletAcctId, rusdAddr, 
				buyStockKeccak, paramTypes, params, note);
	}
	
	/** Sell stock with either BUSD OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD) */
	public static String sellStock(String userAddr, String stablecoinAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
		Util.require( stablecoinAddr.equals( Fireblocks.rusdAddr), "Only RUSD is supported for stock token sales");
		
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				stablecoinAddr, 
				stockTokenAddr, 
				toStablecoin( stablecoinAddr, stablecoinAmt), 
				toStockToken( stockTokenAmt) 
			};
		
		return Fireblocks.call( Fireblocks.refWalletAcctId1, Fireblocks.rusdAddr, 
				sellStockKeccak, paramTypes, params, "RUSD.sellStock()");
	}
	
	/** Amount gets rounded to three decimals */
	public static BigInteger toStockToken(double stockTokenAmt) {
		return timesPower( stockTokenAmt, StockToken.stockTokenDecimals);
	}
	
	/** This method rounds stablecoinAmt to two decimals and converts to integer. */ 
	public static BigInteger toStablecoin(String stablecoinAddr, double stablecoinAmt) throws RefException {
		return timesPower( stablecoinAmt, Fireblocks.getStablecoinMultiplier( stablecoinAddr) ); 
	}

	/** Return amt rounded to three decimals * 10^power */
	private static BigInteger timesPower(double amt, int power) {
		return new BigDecimal( S.fmt3( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}


//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount

	/** There is a RUSD.buyRusd method but it is for the future, currently never called. */
	static void buyRusd(String userAddr, String otherStablecoin, int amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256" };
		Object[] params = { userAddr, otherStablecoin, amt };

		String id = Fireblocks.call( Fireblocks.refWalletAcctId1, Fireblocks.rusdAddr, 
				buyRusdKeccak, paramTypes, params, "RUSD.buyRusd");

		Fireblocks.getTransaction( id).display();
	}

	/** Approve spendingAddr to spend amt RUSD on behalf of account */ 
	public static String approveToSpendRUSD(int account, String spenderAddr, double amt) throws Exception {
		return approve( account, spenderAddr, Fireblocks.rusdAddr, amt);
	}
	
	/** Let account approve spendingAddr to spend amt stablecoin on behalf of account */ 
	public static String approve(int account, String spenderAddr, String stablecoinAddr, double amt) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				spenderAddr, 
				toStablecoin( stablecoinAddr, amt), 
			};
		
		S.out( "Account %s approving %s to spend %s %s", account, spenderAddr, toStablecoin( stablecoinAddr, amt), stablecoinAddr);
		return Fireblocks.call( account, stablecoinAddr, Rusd.approveKeccak, paramTypes, params, "Rusd.approve()");
	}
}
