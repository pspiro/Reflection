package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import json.MyJsonObject;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class StockToken {
	// method signature: buy(address,uint256,uint256,address)
	// test system
	//static String qqq = "0xd1b41cefda7d036018a9daff9d5f4cc811770efb";
	public static String qqq = "0x561fe914443574d2aF7203dCA1ef120036514f87";
	public static String ge =  "0x17211791ea7529a18f18f9247474338a5aee226b"; // on polygon, 18 dec
	public static String ibm = "0xdfff5c453e63facda29b44260c2f5b62b2acd131"; // on polygon, 18 dec
	public static String stk1 = "";
	public static String stk2 = "";
	static final int stockTokenDecimals = 18;
	
	
	static int decimals = 5;
	static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	static String buyKeccak = "3f60b633";
	static String getcallerKk = "a5905412";
	static String iscallerKk = "ac55c8b0";
	
	// 0: default
	// 1: test  (failed, insuf funds)
	// 2: owner
	// 3: refwallet
	// i passed refWallet but it acted like i used Test1 account; that was the sending address

	public static void main(String[] args) throws Exception {
		Fireblocks.setProdValsPolygon();
		//deploy2("Stock2", "Stock2");
	}
	
	static String deploy(String filename, String name, String symbol, String rusdAddr) throws Exception {
		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol,
				rusdAddr
			};
		return Deploy.deploy(filename, Fireblocks.ownerAcctId, paramTypes, params, "deploy stock token " + symbol);
	}
	
	static void deploy2(String name, String symbol) throws Exception {
		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol, 
				Fireblocks.rusdAddr 
			};
		String addr = Deploy.deploy("c:/work/smart-contracts/bytecode/StockToken.bytecode",
				Fireblocks.ownerAcctId, paramTypes, params, "deploy stock");
		S.out( "Deployed to %s", addr);
	}
	
	// now test out all the stock and rusd methods,
	// now that the contracts are deployed
	
	static void buy( String userAddr, int stockTokenAmt, String stockTokenAddr, int stablecoinAmt, String stablecoinAddr) throws Exception {
		String[] paramTypes = { "address", "uint256", "uint256", "address" };
        Object[] params = { userAddr, stockTokenAmt, stablecoinAmt, stablecoinAddr };  
        String id = Fireblocks.call( Fireblocks.refWalletAcctId1, stockTokenAddr, 
        		buyKeccak, paramTypes, params, "StockToken.buy");
		Fireblocks.getTransaction(id).display(); 
	}

	
	private static String pad(double amt) {
		BigInteger big = new BigDecimal(amt).multiply(mult).toBigInteger();
		S.out( "big: %s", big);
		return pad( String.format( "%x", big) );
	}
	
	private static String padAddr(String addr) throws RefException {
		Main.require( addr != null && addr.length() == 42, RefCode.UNKNOWN, "Invalid address %s", addr);
		return pad( addr.substring(2) );
	}

	private static String pad(String str) {
		return Util.padLeft( str, 64, '0'); 
	}
}
