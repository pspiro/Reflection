package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import json.MyJsonObject;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Rusd {
	
	// busd on binance and ethereum has 18 decimals
	// rusd on binance and ethereum has 6 decimals
	// usdc on ethereum has 6 decimals
	// stock tokens have 6 decimals
	static final int stockTokenDecimals = 6;
	
	
	// keccaks
	static final String approveKeccak = "095ea7b3";  // call this on BUSD
	static final String buyRusdKeccak = "28c4ef43"; // this can't be right, we never buy rusd. pas // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static final String buyStockKeccak = "58e78a85";
	static final String sellStockKeccak = "5948f1f0";
	static final String mintKeccak = "40c10f19";

	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	static final BigDecimal ten = new BigDecimal(10);
	
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setProdVals();
		//Fireblocks.setTestVals();
		
		deploy();
//		buyStock("0xb016711702D3302ceF6cEb62419abBeF5c44450e", Fireblocks.rusdAddr,	.01,
//				"0x561fe914443574d2aF7203dCA1ef120036514f87", .01);
				
		// this works in production!
		//mint( "0xb016711702D3302ceF6cEb62419abBeF5c44450e", .01);
		
	}
	
	// this works in test system, fails in production
	static void deploy() throws Exception {
		S.out( "Deploying RUSD from owner %d with refWallet %s", Fireblocks.ownerAcctId, Fireblocks.refWalletAddr);
		String[] paramTypes = { "address" };
		Object[] params = { Fireblocks.refWalletAddr };

		String addr = Deploy.deploy( "c:/work/smart-contracts/rusd.bytecode", 
				Fireblocks.ownerAcctId, paramTypes, params, "Deploy RUSD");
		
		S.out( "Deployed to %s", addr);
	}
	
	private static void mint(String address, double amt) throws Exception {
		String[] types = {"address", "uint256"};
		Object[] vals = {
				address,
				Rusd.toStablecoin(Fireblocks.busdAddr, amt)
		};
		
		String id = Fireblocks.call( Fireblocks.refWalletAcctId, Fireblocks.rusdAddr, mintKeccak, types, vals, "RUSD.mint");
		Fireblocks.getTransHash( id, 60);
	}
	

	/** Buying stock with either BUSD OR RUSD; need to test it both ways.
	 * 
	 *  IMPORTANT, READ THIS FOR FOR TROUBLE-SHOOTING
	 *  
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD)
	 *  and you must have enough base coin in the refWallet */
	public static String buyStock(String userAddr, String stablecoinAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
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
				params[4], stockTokenAddr, params[3], Fireblocks.getStablecoinName(stablecoinAddr), userAddr);
		return Fireblocks.call( Fireblocks.refWalletAcctId, Fireblocks.rusdAddr, 
				buyStockKeccak, paramTypes, params, "RUSD.buyStock()");
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
		
		return Fireblocks.call( Fireblocks.refWalletAcctId, Fireblocks.rusdAddr, 
				sellStockKeccak, paramTypes, params, "RUSD.sellStock()");
	}
	
	/** Amount gets rounded to three decimals */
	public static BigInteger toStockToken(double stockTokenAmt) {
		return timesPower( stockTokenAmt, stockTokenDecimals);
	}
	
	/** This method rounds stablecoinAmt to two decimals and converts to integer. */ 
	public static BigInteger toStablecoin(String stablecoinAddr, double stablecoinAmt) throws RefException {
		return timesPower( stablecoinAmt, getStablecoinMultiplier( stablecoinAddr) ); 
	}

	/** Return amt rounded to three decimals * 10^power */
	private static BigInteger timesPower(double amt, int power) {
		return new BigDecimal( S.fmt3( amt) )
				.multiply( ten.pow( power) )
				.toBigInteger();
	}

	/** Returns the number of decimals of the stablecoin smart contract */
	private static int getStablecoinMultiplier(String stablecoinAddr) throws RefException {
		if (stablecoinAddr.equals(Fireblocks.rusdAddr) ) return 6;  // this will change, all return 18
		if (stablecoinAddr.equals(Fireblocks.busdAddr) ) return 18;  // this will change, all return 18
		throw new RefException( RefCode.UNKNOWN, "Invalid stablecoin address " + stablecoinAddr);
	}

//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount

	/** There is a RUSD.buyRusd method but it is for the future, currently never called. */
	static void buyRusd(String userAddr, String otherStablecoin, int amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256" };
		Object[] params = { userAddr, otherStablecoin, amt };

		String id = Fireblocks.call( Fireblocks.refWalletAcctId, Fireblocks.rusdAddr, 
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
		
		S.out( "Account %s approving %s to spend %s %s", account, spenderAddr, toStablecoin( stablecoinAddr, amt), Fireblocks.getStablecoinName( stablecoinAddr) );
		return Fireblocks.call( account, stablecoinAddr, Rusd.approveKeccak, paramTypes, params, "Rusd.approve()");
	}
}
