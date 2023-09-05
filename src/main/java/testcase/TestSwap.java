package testcase;

import static fireblocks.Accounts.instance;

import fireblocks.Busd;
import fireblocks.Rusd;
import fireblocks.StockToken;
import tw.util.S;

public class TestSwap extends MyTestCase {
	
    String wallet = "0xb95bf9C71e030FA3D8c0940456972885DB60843F";
    String goog = "0x37e9666233833E8e366b2dd25Ac0f329Fd3979B1";
    String aapl = "0x5195729466E481de3c63860034Fc89EFA5FbBB8f";

	public void testSwap() throws Exception {
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
	
    	
}
