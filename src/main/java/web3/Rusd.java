package web3;

import common.Util;
import fireblocks.RetVal;

/** Created by Config which knows which type of core to pass in 
 *  and what params are needed in Web3Params */
public class Rusd extends Stablecoin {
	private IRusd m_core;
	private String m_adminKey;
	
	public Rusd( String rusdAddr, int rusdDecimals, String adminKey, IRusd core) throws Exception {
		super( rusdAddr, rusdDecimals, "RUSD");
		m_adminKey = adminKey;
		m_core = core;
		Util.require( rusdDecimals == 6, "Wrong number of decimals for RUSD " + rusdDecimals);
	}
	
//	public String deploy(String filename, String refWallet, String adminAddr) throws Exception {
//		return m_core.deploy( filename, refWallet, adminAddr);
//	}

	// not used, remove
	public Rusd(String string, int i) throws Exception {
		super( null, 0, null);
		// TODO Auto-generated constructor stub
	}

	public RetVal buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return m_core.buyStock( m_adminKey, userAddr, stablecoin, stablecoinAmt, stockToken, stockTokenAmt);
	}

	public RetVal sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return m_core.sellStockForRusd( m_adminKey, userAddr, rusdAmt, stockToken, stockTokenAmt);
	}

	public RetVal sellRusd(String userAddr, Busd busd, double amt) throws Exception {
		return m_core.sellRusd( m_adminKey, userAddr, busd, amt);
	}


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
		//String deploy(String filename, String refWallet, String adminAddr) throws Exception;
		RetVal buyStock( String adminKey, String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception;
		RetVal sellStockForRusd( String adminKey, String userAddr, double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception;
		RetVal sellRusd( String adminKey, String userAddr, Busd Busd, double amt) throws Exception;
	}

	public void deploy(String string, String address, String address2) {
		//n/a
	}

	public void addOrRemoveAdmin(String address, boolean add) {
		// na
		
	}

}
