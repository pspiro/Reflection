package fireblocks;

import common.Util;
import tw.util.S;
import web3.Erc20;
import web3.RetVal;
import web3.StockToken;

public class FbStockToken extends FbErc20 {
	public static final int stockTokenDecimals = 18;
	static final String setRusdKeccak = "5f4fb7e5";
	
	public FbStockToken(StockToken st) throws Exception {
		super(st.address(), st.decimals(), st.name() );
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

		return FbErc20.deploy(
				filename, 
				Accounts.instance.getId("Owner"), 
				paramTypes, 
				params, 
				"deploy stock token " + symbol
		);
	}
	
	/** This is never called in real life */
	public RetVal setRusdAddress(int fromId, String rusdAddr) throws Exception {
		
		Util.isValidAddress( rusdAddr);
		
		String[] paramTypes = { "address" };
		Object[] params = { rusdAddr };
		
		S.out( "Account %s setting RUSD address on stock token %s to %s",
				fromId, m_address, rusdAddr);

		return Fireblocks.call2(
				fromId,
				m_address, 
				setRusdKeccak, 
				paramTypes, 
				params, 
				"set RUSD address");
	}
}
