package web3;

import org.json.simple.JsonObject;

import chain.Chain;
import chain.ChainParams;
import common.Util;
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
	public static final String setRefWalletKeccak = "0x3a68a420";
	public static final String swapKeccak = "0x62835413";

	private Chain m_chain;
	private boolean m_useAdmin1; // used by RefAPI
	
	public Rusd( String rusdAddr, int rusdDecimals, Chain chain) throws Exception {
		super( rusdAddr, rusdDecimals, "RUSD", chain);
		
		Util.require( rusdDecimals == 6, "Wrong number of decimals for RUSD " + rusdDecimals);

		m_chain = chain;
	}

	/** Only RefAPI should use admin1; all other apps should use sysAdmin */
	public void useAdmin1() {
		m_useAdmin1 = true;
	}
	
	private ChainParams params() {
		return m_chain.params();
	}

	private String adminKey() throws Exception {
		return m_useAdmin1 ? params().admin1Key() : params().sysAdminKey(); 
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
	 *  and you must have enough base coin in the calling wallet */
	public RetVal buyStockWithRusd(String userAddr, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		return buyStock( 
				userAddr,
				this,
				stablecoinAmt,
				stockToken,
				stockTokenAmt
		).crucial();
	}
	
	/** methods to change the smart contract are passed to the core
	 *  @submit if true, transaction is submitted to blockchain; if false, we are just testing to see if it would succeed */
	public RetVal buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( adminKey() );
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
			adminKey(),
			address(),
			buyStockKeccak,
			params,
			500000).crucial();
	}

	public void preBuyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( adminKey() );
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
		Util.reqValidKey( adminKey() );
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
				adminKey(),
				address(),
				sellStockKeccak,
				params,
				500000).crucial();
	}

	public void preSellStock(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
		Util.reqValidKey( adminKey() );
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
		Util.reqValidKey(adminKey() );
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
				adminKey(),
				address(),
				sellRusdKeccak,
				params,
				500000).crucial();
	}

	/** switch this over to Node.callSigned(), then remove contract.exec */
	public RetVal addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception {
		Util.reqValidKey(ownerKey);
		Util.reqValidAddress(address);

		S.out( "RB-RUSD %s %s admin %s",
				Util.getAddress( ownerKey),
				add ? "adding" : "removing",
				address);
		
		Param[] params = {
				new Address( address),
				new BigInt( add ? 1 : 0)  // bool encoded as 1 or 0
		};

		return m_chain.node().callSigned(
				ownerKey,
				address(),
				addOrRemoveKeccak,
				params,
				500000);
	}

	public RetVal swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
		throw new Exception(); // not implemented yet
	}

	/** switch this over to Node.callSigned(), then remove contract.exec
	 * 
	 *  This was never tested */
	public RetVal setRefWallet( String ownerKey, String refWalletAddr) throws Exception {
		Util.reqValidKey(ownerKey);
		Util.reqValidAddress(refWalletAddr);
		
		S.out( "RB-RUSD setting RefWallet to %s", refWalletAddr);

		Param[] params = {
				new Address( refWalletAddr)
		};

		return m_chain.node().callSigned(
				ownerKey,
				address(),
				setRefWalletKeccak,
				params,
				500000);
	}


	/** NOTE: ZkSync doesn't use this; see Deploy.java */
	public String deploy( String ownerKey) throws Exception {
		String file = "C:\\Work\\smart-contracts\\build\\contracts\\RUSD.json";
		String bytecode = JsonObject.readFromFile( file).getString( "bytecode");
		Util.require( S.isNotNull( bytecode), "no bytecode");
		Util.require( (bytecode.length() - 2) % 2 == 0, "bytecode length must be divisible by 2");

		S.out( "deploying RUSD from " + file);
		
		Param[] params = {
				new Address( m_chain.params().refWalletAddr() ),
				new Address( m_chain.params().admin1Addr() )
		};

		String address = m_chain.node().deploy( 
				m_chain.params().ownerKey(), 
				bytecode, 
				Param.encodeParams( params) );

		// confirm that we can interact w/ contract 
		Util.require( m_chain.node().getTokenDecimals( address) == m_decimals, "wrong number of decimals");
		return address;
	}
	
}
