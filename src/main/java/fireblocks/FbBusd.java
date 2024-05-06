package fireblocks;

import tw.util.S;
import web3.Busd.IBusd;

/** This class represents any non-RUSD stablecoin */ 
public class FbBusd extends FbErc20 implements IBusd {
	public FbBusd( String address, int decimals, String name) throws Exception {
		super( address, decimals, name);
	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal mint(String address, double amt) throws Exception {
		return super.mint( Accounts.instance.getId( "Owner"), address, amt);
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	public String deploy(String filename) throws Exception {
		S.out( "Deploying %s from owner", name() );
		
		return deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

	@Override public RetVal approve(String spenderAddr, double amt) throws Exception {
		return null;
	}

}
