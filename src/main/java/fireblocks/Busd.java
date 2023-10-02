package fireblocks;

import tw.util.S;

/** This class represents any non-RUSD stablecoin */ 
public class Busd extends Erc20 {
	
	public Busd( String address, int decimals) throws Exception {
		super( address, decimals, "BUSD");
	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal mint(String address, double amt) throws Exception {
		return super.mint( Accounts.instance.getId( "Owner"), address, amt);
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	void deploy(String filename) throws Exception {
		S.out( "Deploying %s from owner", getName() );
		
		m_address = deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

}
