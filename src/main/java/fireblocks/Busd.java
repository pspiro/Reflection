package fireblocks;

import positions.MoralisServer;
import reflection.Config;
import reflection.Main;
import tw.util.S;

/** This class represents any non-RUSD stablecoin */ 
public class Busd extends Erc20 {
	static final String mintKeccak = "40c10f19";
	
//	public static void main(String[] args) throws Exception {
//		Accounts.instance.setAdmins( "Admin1,Admin2");
//		
//		Config config = new Config();
//		config.readFromSpreadsheet("Test-config");
//		
//		Accounts.instance.read();
//
//		Busd busd = config.newBusd();
//		busd.mint( 
//				Accounts.instance.getId( "Owner"),
//				"0x6DEC8dE8B148952584f52F9AC89A2d6A5A26932f",
//				1);
//		
//	}

	public Busd( String address, int decimals) {
		super( address, decimals);
	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal mint(int accountId, String address, double amt) throws Exception {
		S.out( "Account %s minting %s BUSD for %s", accountId, amt, address);
		
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				address, 
				toBlockchain( amt) 
		};
		
		return Fireblocks.call2( accountId, m_address, mintKeccak, paramTypes, params, "BUSD mint");
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	void deploy(String filename) throws Exception {
		S.out( "Deploying BUSD from owner");
		
		m_address = Deploy.deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

	public double getAllowance(String wallet, String spender) throws Exception {
		return fromBlockchain( MoralisServer.reqAllowance(m_address, wallet, spender).getString("allowance") );
	}
	
}
