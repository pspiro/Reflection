package fireblocks;

import java.math.BigDecimal;

import common.Util;
import reflection.Config;
import testcase.Cookie;
import tw.google.GTable;
import tw.google.NewSheet;
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
	public static final int stockTokenDecimals = 18;
	
	
	static int decimals = 5;
	static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	
	// String m_rusdAddress; // this could/should be a member var
	
	public StockToken( String address) throws Exception {    // you might want to add the name here
		super( address, stockTokenDecimals);
	}
	
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");

		GTable tab = new GTable( NewSheet.Reflection, config.symbolsTab(), "ContractSymbol", "TokenAddress");
		
		// mint 1000 IBM stock token into cookie wallet
		String id = config.rusd().buyStockWithRusd( 
				Cookie.wallet,
				0,
				new StockToken( tab.get( "IBM") ),
				1000);
		Fireblocks.waitForTransHash(id, 60, 1000);
	}
	
	// 0: default
	// 1: test  (failed, insuf funds)
	// 2: owner
	// 3: refwallet
	// i passed refWallet but it acted like i used Test1 account; that was the sending address

	public static StockToken deploy(String filename, String name, String symbol, String rusdAddr) throws Exception {
		Util.require( S.isNotNull( name), "Null name" );
		Util.require( S.isNotNull( symbol), "Null symbol" );
		Util.require( S.isNotNull( rusdAddr), "Null rusdAddr" );

		S.out( "Deploying stock token from %s  name=%s  symbol=%s", filename, name, symbol);
		
		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol,
				rusdAddr
		};

		String address = deploy(
				filename, 
				Accounts.instance.getId("Owner"), 
				paramTypes, 
				params, 
				"deploy stock token " + symbol
		);
		return new StockToken( address);
	}

	/** Amount gets rounded to three decimals */
//	public static BigInteger toStockToken(double stockTokenAmt) {
//		return Rusd.timesPower( stockTokenAmt, stockTokenDecimals);
//	}
}
