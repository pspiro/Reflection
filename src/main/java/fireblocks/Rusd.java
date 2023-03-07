package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import reflection.Config;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Rusd extends Erc20 {
	
	// busd on binance and ethereum has 18 decimals
	// usdc and usdt on ethereum have 6 decimals
	// stock tokens have 18 decimals
	
	
	// keccaks
	static final String buyRusdKeccak = "28c4ef43"; // this can't be right, we never buy rusd. pas // buyRusd(address userAddress, address stableCoinAddress, uint256 amount)
	static final String sellRusdKeccak = "054d4a45";
	static final String buyStockKeccak = "58e78a85";
	static final String sellStockKeccak = "5948f1f0";
	static final String burnKeccak = "9dc29fac";
	static final String addOrRemoveKeccak = "89fa2c03";

	// deploy RUSD from owner wallet
	// deploy QQQ from owner wallet
	
	static String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	//you have to approve THE CONTRACT that will be calling the methods on busd or rusd
	
	private int stockTokenDecimals;
	
//	String rusdAddr = "0x31ed1e80db8a6e82b2f73c4cb37a1390fe7793a7"; // deploy( "c:/work/bytecode/rusd.bytecode");
//	String ibmAddr = "0xfdaf3b9c6665fe47eb701abea7429d0c1b5d30a1"; // StockToken.deploy( "c:/work/bytecode/stocktoken.bytecode", "IBM", "IBM", rusdAddr);
	
	public Rusd( String rusdAddr, int rusdDecimals, int stockTokenDecimals) {
		super( rusdAddr, rusdDecimals);
		this.stockTokenDecimals = stockTokenDecimals;
	}
	
	/** Deploy RUSD
	 *  @return deployed address */
	void deploy(String filename, String refWallet, String admin) throws Exception {
		S.out( "Deploying RUSD from owner with refWallet %s and admin %s", refWallet, admin);
		String[] paramTypes = { "address", "address" };
		Object[] params = { refWallet, admin };
		
		m_address = Deploy.deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				paramTypes, 
				params, 
				"Deploy RUSD"
		);
	}
	

	/** Buying stock with either BUSD OR RUSD; need to test it both ways.
	 * 
	 *  IMPORTANT, READ THIS FOR FOR TROUBLE-SHOOTING
	 *  
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD)
	 *  and you must have enough base coin in the refWallet */
	public String buyStockWithRusd(int adminAcctId, String userAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
		return buyStockWithStablecoin( 
				adminAcctId, 
				userAddr,
				m_address, 
				m_decimals, 
				stablecoinAmt,
				stockTokenAddr,
				stockTokenAmt
		);
	}
	
	/** Buy with either RUSD or BUSD */
	public String buyStockWithStablecoin(int adminAcctId, String userAddr, String stablecoinAddr, int stablecoinDecimals, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };
		Object[] params = { 
				userAddr,
				stablecoinAddr, 
				stockTokenAddr,
				timesPower( stablecoinAmt, stablecoinDecimals),
				StockToken.toStockToken( stockTokenAmt) 
			};
		
		// you should check (a) that approval was done, and (b) that there is
		// sufficient coin in the source wallet. pas
		
		S.out( "Refwallet buying %s %s with %s %s for user %s", 
				params[4], stockTokenAddr, params[3], stablecoinAddr, userAddr);
		return Fireblocks.call( adminAcctId, m_address, 
				buyStockKeccak, paramTypes, params, "RUSD buy stock");
	}
	
	/** Sell stock with either BUSD OR RUSD; need to try it both ways.
	 *  Whichever one your are buying with, you must have enough in User wallet
	 *  and you must be approved (if buying with BUSD) */
	public String sellStockForRusd(int adminAcctId, String userAddr, double stablecoinAmt, String stockTokenAddr, double stockTokenAmt) throws Exception {
		String[] paramTypes = { "address", "address", "address", "uint256", "uint256" };

		Object[] params = { 
				userAddr, 
				m_address, 
				stockTokenAddr, 
				toBlockchain( stablecoinAmt), 
				StockToken.toStockToken( stockTokenAmt) 
		};
		
		return Fireblocks.call( adminAcctId, m_address, 
				sellStockKeccak, paramTypes, params, "RUSD sell stock");
	}
	
	/** There is a RUSD.buyRusd method but it is for the future, currently never called. */
	public String sellRusd(int adminAcctId, String userAddr, String busdAddr, int amt) throws Exception {
		String[] paramTypes = { "address", "address", "uint256" };
		Object[] params = { userAddr, busdAddr, amt };

		return Fireblocks.call( 
				adminAcctId, 
				m_address, 
				sellRusdKeccak, 
				paramTypes, 
				params, 
				"sell RUSD");
	}

	


//    address _userAddress,
//    address _stableCoinAddress,
//    uint256 _amount

//	static void buyRusd(String userAddr, String otherStablecoin, int amt) throws Exception {
//		String[] paramTypes = { "address", "address", "uint256" };
//		Object[] params = { userAddr, otherStablecoin, amt };
//
//		String id = Fireblocks.call( Fireblocks.refWalletAcctId1, Fireblocks.rusdAddr, 
//				buyRusdKeccak, paramTypes, params, "RUSD.buyRusd");
//
//		Fireblocks.getTransaction( id).display();
//	}

	public void addOrRemoveAdmin(int callerAcctId, String adminAddr, boolean add) throws Exception {
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				adminAddr, 
				1 
		};
		
		Fireblocks.call( callerAcctId, m_address, addOrRemoveKeccak, paramTypes, params, "RUSD add admin");
	}

}
