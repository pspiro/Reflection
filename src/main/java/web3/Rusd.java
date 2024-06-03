package web3;

import common.Util;

/** The Rusd class used by clients. Created by Config which knows which 
 *  type of core to pass in */
public class Rusd extends Stablecoin {
	private IRusd m_core;
	private String m_adminKey; // for Fireblocks, this is the name; for Refblocks, this is private key
	
	public Rusd( String rusdAddr, int rusdDecimals, String adminKey, IRusd core) throws Exception {
		super( rusdAddr, rusdDecimals, "RUSD");
		
		Util.require( rusdDecimals == 6, "Wrong number of decimals for RUSD " + rusdDecimals);
		Util.require( core != null, "null core");

		m_adminKey = adminKey;
		m_core = core;
	}

	/** methods to change the smart contract are passed to the core */
	public RetVal buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return m_core.buyStock( m_adminKey, userAddr, stablecoin, stablecoinAmt, stockToken, stockTokenAmt);
	}

	/** methods to change the smart contract are passed to the core */
	public RetVal sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return m_core.sellStockForRusd( m_adminKey, userAddr, rusdAmt, stockToken, stockTokenAmt);
	}

	/** methods to change the smart contract are passed to the core */
	public RetVal sellRusd(String userAddr, Busd busd, double amt) throws Exception {
		return m_core.sellRusd( m_adminKey, userAddr, busd, amt);
	}

	public RetVal addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception {
		return m_core.addOrRemoveAdmin( ownerKey, address, add);
	}

	public RetVal swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
		return m_core.swap( userAddr, stockToBurn, stockToMint, burnAmt, mintAmt);
	}

//	public RetVal approve( String ownerKey, String spender, double amt) throws Exception {
//		return m_core.approve( ownerKey, spender, amt);
//	}

	// real methods are implemented here

	/** Buying stock with either FBusd OR RUSD; need to test it both ways.
	 * 
	 *  IMPORTANT, READ THIS FOR FOR TROUBLE-SHOOTING
	 *  
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with Busd)
	 *  and you must have enough base coin in the refWallet */
	public RetVal buyStockWithRusd(String userAddr, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return buyStock( 
				userAddr,
				this,
				stablecoinAmt,
				stockToken,
				stockTokenAmt
		);
	}
	
	/** RUSD has no mint function, so we sell zero shares of stock */
	public RetVal mintRusd(String address, double amt, StockToken anyStockToken) throws Exception {
		return sellStockForRusd( address, amt, anyStockToken, 0);
	}

	/** RUSD has no mint function, so we sell zero shares of stock */
	public RetVal burnRusd(String address, double amt, StockToken anyStockToken) throws Exception {
		return buyStockWithRusd( address, amt, anyStockToken, 0);
	}
	
	public RetVal mintStockToken(String address, StockToken stockToken, double amt) throws Exception {
		return buyStockWithRusd(address, 0, stockToken, amt);
	}
	
	public interface IRusd {
		RetVal buyStock( String adminKey, String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception;
//		RetVal approve(String ownerKey, String spender, double amt) throws Exception;
		RetVal sellStockForRusd( String adminKey, String userAddr, double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception;
		RetVal sellRusd( String adminKey, String userAddr, Busd Busd, double amt) throws Exception;
		RetVal addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception;
		RetVal swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception;
	}
}
