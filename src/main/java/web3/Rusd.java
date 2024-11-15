package web3;

import chain.Chain;
import chain.ChainParams;
import common.Util;
import refblocks.Refblocks;
import tw.util.S;

/** The Rusd class used by clients. Created by Config which knows which 
 *  type of core to pass in */
public class Rusd extends Stablecoin {
	public static final String buyRusdKeccak = "8a854e17";
	public static final String sellRusdKeccak = "5690cc4f"; 
	public static final String buyStockKeccak = "58e78a85";
	public static final String sellStockKeccak = "5948f1f0";
	public static final String addOrRemoveKeccak = "89fa2c03";
	public static final String swapKeccak = "62835413";

	
	private Chain m_chain;
	
	public Rusd( String rusdAddr, int rusdDecimals, Chain chain) throws Exception {
		super( rusdAddr, rusdDecimals, "RUSD", chain);
		
		Util.require( rusdDecimals == 6, "Wrong number of decimals for RUSD " + rusdDecimals);

		m_chain = chain;
	}
	
	ChainParams params() {
		return m_chain.params();
	}

	private String admin1Key() throws Exception {
		return params().admin1Key(); 
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

	/** methods to change the smart contract are passed to the core */
	public RetVal buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);
		
		S.out( "RB-RUSD buyStock %s %s paying %s %s for user %s", 
				stockTokenAmt,
				stockToken.address(),
				stablecoinAmt,
				stablecoin.name(),
				userAddr);
		
		var contract = load( admin1Key() );
		return contract.exec( () -> contract.buyStock(
				userAddr, 
				stablecoin.address(), 
				stockToken.address(), 
				stablecoin.toBlockchain( stablecoinAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				) );

	}

	/** methods to change the smart contract are passed to the core */
	public RetVal sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);

		S.out( "RB-RUSD sellStock %s %s receive %s RUSD for user %s",
				stockTokenAmt,
				stockToken.name(),
				rusdAmt,
				userAddr);

		var contract = load( admin1Key() );
		return contract.exec( () -> contract.sellStock( 
				userAddr,
				address(), 
				stockToken.address(), 
				toBlockchain( rusdAmt), 
				stockToken.toBlockchain( stockTokenAmt)
				) );

	}

	/** methods to change the smart contract are passed to the core */
	public RetVal sellRusd(String userAddr, Busd busd, double amt) throws Exception {
		Util.reqValidKey(admin1Key() );
		Util.reqValidAddress(userAddr);

		S.out( "RUSD redeeming %s RUSD receive %s %s for user %s",
				S.fmt6( amt),
				S.fmt6( amt),
				busd.name(),
				userAddr);
		
		//S.out( "  the allowance is %s", )

		var contract = load( admin1Key() );
		return contract.exec( () -> contract.sellRusd(
				userAddr,
				busd.address(),
				busd.toBlockchain( amt),
				toBlockchain( amt)
				) );
	}

	public RetVal addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception {
		Util.reqValidKey(ownerKey);
		Util.reqValidAddress(address);

		S.out( "RB-RUSD %s %s admin %s",
				Util.getAddress( ownerKey),
				add ? "adding" : "removing",
				address);
				
		var contract = load( ownerKey);
		return contract.exec( () -> contract.addOrRemoveAdmin( address,	add) );
	}

	public RetVal swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
		throw new Exception(); // not implemented yet
	}

	public RetVal setRefWallet( String ownerKey, String refWalletAddr) throws Exception {
		Util.reqValidKey(ownerKey);
		Util.reqValidAddress(refWalletAddr);
		
		S.out( "RB-RUSD setting RefWallet to %s", refWalletAddr);
		
		var contract = load( ownerKey);
		return contract.exec( () -> contract.setRefWalletAddress( refWalletAddr) );
	}


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
	
	/** load generated Rusd that we can use to call smart contract methods that write to the blockchain
	 *  note that we no longer need to have tm passed in because we can get it from Refblocks */
	public refblocks.Rusd load(String callerKey) throws Exception {
		return refblocks.Rusd.load( 
				address(), 
				m_chain.web3j(), 
				m_chain.blocks().getFasterTm( callerKey), 
				m_chain.blocks().getGp( 500000)  // this is good for everything except deployment
				);
	}

	public String deploy( String ownerKey, String refWallet, String admin1) throws Exception {
		S.out( "deploying RUSD");
		
		return refblocks.Rusd.deploy( 
				m_chain.web3j(),
				m_chain.blocks().getWaitingTm( ownerKey),
				m_chain.blocks().getGp( Refblocks.deployGas),
				refWallet, admin1
				).send().getContractAddress();
	}
	
}
