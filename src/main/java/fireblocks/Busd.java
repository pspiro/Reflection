package fireblocks;

import reflection.Config;
import tw.util.S;

/** This class represents any non-RUSD stablecoin */ 
public class Busd extends Erc20 {
	static final String mintKeccak = "40c10f19";
	
	public static void main(String[] args) throws Exception {
		Accounts.instance.setAdmins( "Admin1,Admin2");
		//Accounts.instance.read();
		
		Config config = Config.readFrom("Dev-config");

		Busd busd = config.busd();
		busd.mint( 
				Accounts.instance.getId( "Owner"),
				"0xb95bf9C71e030FA3D8c0940456972885DB60843F",
				//"0xd953DC148f3A1019132FBD75Ee515E3F786f6634",
				300000);
		
	}

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
		
		return Fireblocks.call2( accountId, m_address, mintKeccak, paramTypes, params, "Stablecoin mint");
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	void deploy(String filename) throws Exception {
		S.out( "Deploying BUSD from owner");
		
		m_address = deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

}
