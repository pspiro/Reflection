package fireblocks;

import json.MyJsonObject;
import tw.util.S;

public class Rusd {
	// test system values
	static int refWalletAcctId = 3;
	static int ownerAcctId = 2;
	static int userWalletId = 1;
	
	// wallet addresses
	static String ownerAddr = "0xdA2c28Af9CbfaD9956333Aba0Fc3B482bc0AeD13";
	static String refWalletAddr = "0x4d2AE56E463bBbd1803DD892a4AF1b7Ce9b65667"; // test system
	static String userAddr = "0xAb52e8f017fBD6C7708c7C90C0204966690e7Fc8";
	static String busdAddr = "0x76CBf8325E0cC59AaD46204C80091757B06b54a3";

	// token addresses
	static String rusdAddr = "0xdd9b1982261f0437aff1d3fec9584f86ab4f8197"; // contract address deployed with this refWallet

	// keccaks
	static String approveKeccak = "095ea7b3";  // call this on BUSD
	static String buyRusdKeccak = "0x28c4ef43"; // this can't be right, we never buy rusd. pas // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static String buyStockKeccak = "0x58e78a85";
	//static String sellStock = "5948f1f0";
	
	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
//		deploy();
		//approveBusd();
		buyStock( userAddr, busdAddr, StockToken.qqq, 10, 11); // this works
		buyStock( userAddr, rusdAddr, StockToken.qqq, 10, 11); // test this
//		buyRusd( userAddr, busdAddr, 9);   // this works, make sure you have called BUSD.approve(RUSD) for spending
	}
	
	static void approveBusd() throws Exception {
		String[] paramTypes = { "address", "uint256" };
		Object[] params = { rusdAddr, 1000 };
		Fireblocks.call( userWalletId, busdAddr, approveKeccak, paramTypes, params, "Busd.approve");
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
	/** Buying stock with either BUSD OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD) */
	public static void buyStock(String userAddr, String stablecoinAddr, String stockTokenAddr, int stablecoinAmt, int stockTokenAmt) throws Exception {
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { userAddr, stablecoinAddr, stockTokenAddr, stablecoinAmt, stockTokenAmt };
		
		MyJsonObject obj = Fireblocks.call( refWalletAcctId, rusdAddr, 
				buyStockKeccak, paramTypes, params, "RUSD.buyStock");
		obj.display();
		
		String id = obj.getString("id");
		Fireblocks.getTransaction( id).display();
	}
//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount

	static void buyRusd(String userAddr, String otherStablecoin, int amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256" };
		Object[] params = { userAddr, otherStablecoin, amt };
		
		MyJsonObject obj = Fireblocks.call( refWalletAcctId, rusdAddr, 
				buyRusdKeccak, paramTypes, params, "RUSD.buyRusd");
		obj.display();
		
		String id = obj.getString("id");
		Fireblocks.getTransaction( id).display();
	}
}
