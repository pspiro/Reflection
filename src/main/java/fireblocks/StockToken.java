package fireblocks;

import common.Util;
import tw.util.S;

public class StockToken extends Erc20 {
	public static String qqq = "0x561fe914443574d2aF7203dCA1ef120036514f87";
	public static String ge =  "0x17211791ea7529a18f18f9247474338a5aee226b"; // on polygon, 18 dec
	public static String ibm = "0xdfff5c453e63facda29b44260c2f5b62b2acd131"; // on polygon, 18 dec
	public static String stk1 = "";
	public static String stk2 = "";
	public static final int stockTokenDecimals = 18;
		   static String setRusdKeccak = "5f4fb7e5";
//		   static int decimals = 5;
//		   static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	
	public StockToken( String address) throws Exception {    // you might want to add the name here
		super( address, stockTokenDecimals, "StockToken");
	}
	
	public RetVal setRusdAddress(int id, String rusdAddr) throws Exception {
		Util.reqValidAddress( rusdAddr);
		String[] paramTypes = { "address" };
		Object[] params = { rusdAddr };
		
		S.out( "Setting RUSD address to %s for stock token %s", rusdAddr, m_address);

		return call( id, setRusdKeccak, paramTypes, params, "StockToken setRusdAddress");		
	}
	
	// 0: default
	// 1: test  (failed, insuf funds)
	// 2: owner
	// 3: refwallet
	// i passed refWallet but it acted like i used Test1 account; that was the sending address

	public static StockToken deploy(String filename, String name, String symbol, String rusdAddr) throws Exception {
		Util.require( S.isNotNull( name), "Null name" );
		Util.require( S.isNotNull( symbol), "Null symbol" );
		Util.require( name.length() <= 32, "Name too long"); // I don't know where this is imposed, but it fails with length > 32
		Util.require( symbol.length() <= 18, "Symbol too long"); // it may work with longer but I only tested up to this point
		Util.reqValidAddress( rusdAddr);

		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol,
				rusdAddr
		};

		S.out( "Deploying stock token from %s  name=%s  symbol=%s", filename, name, symbol);

		String address = deploy(
				filename, 
				Accounts.instance.getId("Owner"), 
				paramTypes, 
				params, 
				"deploy stock token " + symbol
		);
		return new StockToken( address);
	}
}
