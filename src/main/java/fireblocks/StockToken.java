package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import json.MyJsonObject;
import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class StockToken extends Erc20 {
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
	
	StockToken( String address) {
		super( address, stockTokenDecimals);
	}
	
	// 0: default
	// 1: test  (failed, insuf funds)
	// 2: owner
	// 3: refwallet
	// i passed refWallet but it acted like i used Test1 account; that was the sending address

	public static void main(String[] args) throws Exception {
		Fireblocks.setProdValsPolygon();
	}
	
	static StockToken deploy(String filename, String name, String symbol, String rusdAddr) throws Exception {
		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol,
				rusdAddr
		};

		String address = Deploy.deploy(filename, Fireblocks.ownerAcctId, paramTypes, params, "deploy stock token " + symbol);
		return new StockToken( address);
	}

	/** Amount gets rounded to three decimals */
	public static BigInteger toStockToken(double stockTokenAmt) {
		return Rusd.timesPower( stockTokenAmt, stockTokenDecimals);
	}
}
