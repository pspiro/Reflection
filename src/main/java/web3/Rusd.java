package web3;

import chain.Chain;
import chain.ChainParams;
import common.Util;
import refblocks.Refblocks;
import tw.util.S;
import web3.Param.Address;
import web3.Param.BigInt;

/** The Rusd class used by clients. Created by Config which knows which 
 *  type of core to pass in */
public class Rusd extends Stablecoin {
	public static final String buyRusdKeccak = "0x8a854e17";
	public static final String sellRusdKeccak = "0x5690cc4f"; 
	public static final String buyStockKeccak = "0x58e78a85";
	public static final String sellStockKeccak = "0x5948f1f0";
	public static final String addOrRemoveKeccak = "0x89fa2c03";
	public static final String swapKeccak = "0x62835413";

	
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
	
	/** methods to change the smart contract are passed to the core
	 *  @submit if true, transaction is submitted to blockchain; if false, we are just testing to see if it would succeed */
	public RetVal buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);
		
		S.out( "RB-RUSD buyStock %s %s paying %s %s for user %s", 
				stockTokenAmt,
				stockToken.address(),
				stablecoinAmt,
				stablecoin.name(),
				userAddr);
		
		Param[] params = {
				new Address( userAddr),
				new Address( stablecoin.address() ),
				new Address( stockToken.address() ),
				new BigInt( stablecoin.toBlockchain( stablecoinAmt) ), 
				new BigInt( stockToken.toBlockchain( stockTokenAmt) )
		};
		
		return m_chain.node().callSigned(
			admin1Key(),
			address(),
			buyStockKeccak,
			params,
			500000);
	}

	public void preBuyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);
		
		Param[] params = {
				new Address( userAddr),
				new Address( stablecoin.address() ),
				new Address( stockToken.address() ),
				new BigInt( stablecoin.toBlockchain( stablecoinAmt) ), 
				new BigInt( stockToken.toBlockchain( stockTokenAmt) )
		};
		
		m_chain.node().preCheck( params().admin1Addr(), address(), buyStockKeccak, params, 500000);
	}

	public RetVal sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);

		S.out( "RB-RUSD sellStock %s %s receive %s RUSD for user %s",
				stockTokenAmt,
				stockToken.name(),
				rusdAmt,
				userAddr);
		
		Param[] params = {
				new Address( userAddr),
				new Address( address() ), 
				new Address( stockToken.address() ), 
				new BigInt( toBlockchain( rusdAmt) ), 
				new BigInt( stockToken.toBlockchain( stockTokenAmt) )
		};
		
		return m_chain.node().callSigned(
				admin1Key(),
				address(),
				sellStockKeccak,
				params,
				500000);
	}

	public void preSellStock(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( admin1Key() );
		Util.reqValidAddress(userAddr);

		Param[] params = {
				new Address( userAddr),
				new Address( address() ), 
				new Address( stockToken.address() ), 
				new BigInt( toBlockchain( rusdAmt) ), 
				new BigInt( stockToken.toBlockchain( stockTokenAmt) )
		};
		
		m_chain.node().preCheck(
				params().admin1Addr(),
				address(),
				sellStockKeccak,
				params,
				500000);
	}

	/** Sell/redeem RUSD for stablecoin */
	public RetVal sellRusd(String userAddr, Busd busd, double amt) throws Exception {
		Util.reqValidKey(admin1Key() );
		Util.reqValidAddress(userAddr);

		S.out( "RUSD redeeming %s RUSD receive %s %s for user %s",
				S.fmt6( amt),
				S.fmt6( amt),
				busd.name(),
				userAddr);
		
		Param[] params = {
				new Address( userAddr),
				new Address( busd.address() ),
				new BigInt( busd.toBlockchain( amt) ), 
				new BigInt( toBlockchain( amt) )
		};
		
		return m_chain.node().callSigned(
				admin1Key(),
				address(),
				sellRusdKeccak,
				params,
				500000);
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
