package fireblocks;

import common.Util;
import tw.util.S;

public class FbStockToken extends FbErc20 {
	public static final int stockTokenDecimals = 18;
	static final String setRusdKeccak = "5f4fb7e5";
	
	public FbStockToken( String address) throws Exception {
		super( address, stockTokenDecimals, "StockToken");
	}
	
	public static String deploy(String filename, String name, String symbol, String rusdAddr) throws Exception {
		Util.require( S.isNotNull( name), "Null name" );
		Util.require( S.isNotNull( symbol), "Null symbol" );
		Util.require( symbol.length() <= 18, "Symbol too long"); // it may work with longer but I only tested up to this point
		Util.reqValidAddress( rusdAddr);

		String[] paramTypes = { "string", "string", "address" };
		Object[] params = { 
				name, 
				symbol,
				rusdAddr
		};

		S.out( "Deploying stock token from %s  name=%s  symbol=%s", filename, name, symbol);

		return deploy(
				filename, 
				Accounts.instance.getId("Owner"), 
				paramTypes, 
				params, 
				"deploy stock token " + symbol
		);
	}
	
	public RetVal setRusdAddress(String rusdAddr) throws Exception {
		Util.reqValidAddress( rusdAddr);
		String[] paramTypes = { "address" };
		Object[] params = { rusdAddr };
		
		S.out( "Setting RUSD address to %s for stock token %s", rusdAddr, m_address);

		return call( 
				Accounts.instance.getId( "Owner"), 
				setRusdKeccak, 
				paramTypes, 
				params, 
				"StockToken setRusdAddress");		
	}
}
