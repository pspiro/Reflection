package testcase;

import static fireblocks.Accounts.instance;

import fireblocks.Busd;
import fireblocks.Erc20;
import fireblocks.Rusd;
import fireblocks.StockToken;
import tw.util.S;

public class TestSwap extends MyTestCase {
	
    String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
    String goog = "0x37e9666233833E8e366b2dd25Ac0f329Fd3979B1";
    String aapl = "0x5195729466E481de3c63860034Fc89EFA5FbBB8f";

//    public static void main(String[] args) throws Exception {
////    	S.out(
////    		Fireblocks.waitForTransHash("a6c1781b-f0f9-4e1d-bab3-112cc12efde0", 60, 1000)
////    	);    			
//		S.out(
//			Erc20.getDeployedAddress("0x0705c457161a24456433c9c5d3afea2614daeb474d0cf2cc70bfb3a3e636b02d")
//		);
//	}
	public void setupTestSwap() throws Exception {
		S.out( "Swapping RUSD %s", m_config.rusd().address() );
				
		Rusd rusd = new Rusd("", 6);
		Busd busd = m_config.busd();
		
		// deploy RUSD (if set to "deploy")
		rusd.deploy( 
				"c:/work/smart-contracts/build/contracts/rusd.json",
				instance.getAddress( "RefWallet"),
				instance.getAddress( "Admin1")	);

		// let RefWallet approve RUSD to transfer BUSD
		busd.approve( 
				instance.getId( "RefWallet"), // called by
				rusd.address(), // approving
				1000000000); // $1B
		
		StockToken tokenA = StockToken.deploy( 
				"c:/work/smart-contracts/build/contracts/stocktoken.json",						
				"AAA",
				"AAA",
				rusd.address()
		);
				
		StockToken tokenB = StockToken.deploy( 
				"c:/work/smart-contracts/build/contracts/stocktoken.json",						
				"BBB",
				"BBB",
				rusd.address()
		);
		
		rusd.buyStockWithRusd(wallet, 0, tokenA, 25);
		rusd.swap(wallet, tokenA, tokenB, 20, 10);

		//Test.run(config, busd, rusd);
//		RetVal ret = m_config.rusd().swap( 
//				wallet,
//				new StockToken(goog),
//				new StockToken(aapl),
//				1.0,
//				1.0
//			);
//		ret.waitForHash();
	}
	
	public void testSwap() throws Exception {
		S.out( "Swapping RUSD %s", m_config.rusd().address() );
				
		Rusd rusd = new Rusd("0xbac1dbc98f8616dc0f491dc229e28c1911df2b4b", 6);
		Busd busd = m_config.busd();
		
		
		StockToken tokenA = new StockToken("0x9e3503a3aaf159cdd6eec2d225e509fb9bf98fc2"); 
		StockToken tokenB = new StockToken("0x45a8982f82a70723cbe830af3a66eaef7b885a57"); 
		
		//rusd.buyStockWithRusd(wallet, 0, tokenA, 25);
		String hash = rusd.swap(wallet, tokenA, tokenB, 20, 10).waitForHash();
		S.out( hash);
		
		//Test.run(config, busd, rusd);
//		RetVal ret = m_config.rusd().swap( 
//				wallet,
//				new StockToken(goog),
//				new StockToken(aapl),
//				1.0,
//				1.0
//			);
//		ret.waitForHash();
	}
	
    	
}
