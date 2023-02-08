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
	
	// test system values
	public static int refWalletAcctId = 3;
	public static int ownerAcctId = 2;
	public static int userAcctId = 1;
	
	// wallet addresses
	public static String ownerAddr = "0xdA2c28Af9CbfaD9956333Aba0Fc3B482bc0AeD13";
	public static String refWalletAddr = "0x4d2AE56E463bBbd1803DD892a4AF1b7Ce9b65667"; // test system
	public static String userAddr = "0xAb52e8f017fBD6C7708c7C90C0204966690e7Fc8"; // Testnet Test1 account (id=1)
	public static String user2 = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";

	// stablecoin addresses Goerli
	//public static final String busdAddr = "0x76CBf8325E0cC59AaD46204C80091757B06b54a3";
	public static final String busdAddr = "0x833c8c086885f01bf009046279ac745cec864b7d"; // this is our BUSD that I deployed from test.Owner with test.RefWallet as the one who can call mint
	public static final String rusdAddr = "0xdd9b1982261f0437aff1d3fec9584f86ab4f8197"; // contract address deployed with this refWallet

	// keccaks
	static final String approveKeccak = "095ea7b3";  // call this on BUSD
	static final String buyRusdKeccak = "0x28c4ef43"; // this can't be right, we never buy rusd. pas // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static final String buyStockKeccak = "0x58e78a85";
	static final String sellStockKeccak = "0x5948f1f0";
	static final String mintKeccak = "40c10f19";

	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	static final BigDecimal ten = new BigDecimal(10);
	
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 3; i++) {
			new Thread( () -> testone() ).start();
			S.sleep(100);
		}
	}
	
	static void testone() {
		try {
			testonea();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void testonea() throws Exception {
		Fireblocks.setTestVals();
//		deploy();
		//approveBusd();
		//String id = buyStock( userAddr, busdAddr, 10, StockToken.qqq, 11); // this works
		String id = buyStock( userAddr, rusdAddr, 10, StockToken.qqq, 11); // this works
		String hash = Fireblocks.getTransHash(id, 60);  // do we really need to wait this long? pas
		S.out( "%s got hash %s", id, hash);
		
		//buyStock( userAddr, busdAddr, 10, TestFireblocks.qqq, 11); // this works
		//buyStock( userAddr, rusdAddr, 10, StockToken.qqq, 11); // test this
//		buyRusd( userAddr, busdAddr, 9);   // this works, make sure you have called BUSD.approve(RUSD) for spending
		//mint( Rusd.userAddr, 2000);
	}
	
	private static void mint(String address, double amt) throws Exception {
		String[] types = {"address", "uint256"};
		Object[] vals = {
				address,
				Rusd.toStablecoin(Rusd.busdAddr, amt)
		};
		
		Fireblocks.call( Rusd.refWalletAcctId, Rusd.rusdAddr, mintKeccak, types, vals, "RUSD.mint()").display();
	}
	
	// this works
	static void deploy() throws Exception {
		S.out( "Deploying RUSD from owner %d with refWallet %s", ownerAcctId, refWalletAddr);
		String[] paramTypes = { "address" };
		Object[] params = { refWalletAddr };
		String addr = Deploy.deploy( "c:/work/smart-contracts/rusd.bytecode", ownerAcctId, paramTypes, params, "Deploy RUSD");
		S.out( "Deployed to %s", addr);
	}
	
	/*
	 *  function buyStock(
	        address _userAddress,
	        address _stableCoinAddress,
	        address _stockTokenAddress,
	        uint256 _stableCoinAmount,
	        uint256 _stockTokenAmount
	 */

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
				params[4], stockTokenAddr, params[3], getName(stablecoinAddr), userAddr);
		MyJsonObject obj = Fireblocks.call( refWalletAcctId, rusdAddr, 
				buyStockKeccak, paramTypes, params, "RUSD.buyStock()");
		S.out( "%s Buy stock %s", obj.getString("id"), obj.getString("status") );
		return obj.getString("id");
	}
	
	/** Sell stock with either BUSD OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD) */
	public static String sellStock(String userAddr, String stablecoinAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
		Util.require( stablecoinAddr.equals( rusdAddr), "Only RUSD is supported for stock token sales");
		
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				stablecoinAddr, 
				stockTokenAddr, 
				toStablecoin( stablecoinAddr, stablecoinAmt), 
				toStockToken( stockTokenAmt) 
			};
		
		MyJsonObject obj = Fireblocks.call( refWalletAcctId, rusdAddr, 
				sellStockKeccak, paramTypes, params, "RUSD.sellStock()");
		obj.display();
		
		return obj.getString("id");
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
		switch( stablecoinAddr) {
			case rusdAddr: return 6;
			case busdAddr: return 18;
		}
		throw new RefException( RefCode.UNKNOWN, "Invalid stablecoin address");
	}

//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount

	/** There is a RUSD.buyRusd method but it is for the future, currently never called. */
	static void buyRusd(String userAddr, String otherStablecoin, int amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256" };
		Object[] params = { userAddr, otherStablecoin, amt };

		MyJsonObject obj = Fireblocks.call( refWalletAcctId, rusdAddr, 
				buyRusdKeccak, paramTypes, params, "RUSD.buyRusd");
		obj.display();
		
		String id = obj.getString("id");
		Fireblocks.getTransaction( id).display();
	}

	/** Approve spendingAddr to spend amt RUSD on behalf of account */ 
	public static String approveToSpendRUSD(int account, String spenderAddr, double amt) throws Exception {
		return approve( account, spenderAddr, Rusd.rusdAddr, amt);
	}
	
	/** Let account approve spendingAddr to spend amt stablecoin on behalf of account */ 
	public static String approve(int account, String spenderAddr, String stablecoinAddr, double amt) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				spenderAddr, 
				toStablecoin( stablecoinAddr, amt), 
			};
		
		S.out( "Account %s approving %s to spend %s %s", account, spenderAddr, toStablecoin( stablecoinAddr, amt), getName( stablecoinAddr) );
		return Fireblocks.call( account, stablecoinAddr, Rusd.approveKeccak, paramTypes, params, "Rusd.approve()")
				.getString("id");
	}

	private static Object getName(String stablecoinAddr) {
		switch( stablecoinAddr) {
			case rusdAddr: return "RUSD";
			case busdAddr: return "BUSD";
			default: return "???";
		}
	}
}
