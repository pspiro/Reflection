package fireblocks;

import tw.util.S;
import web3.RetVal;
import web3.Busd.IBusd;

/** This class represents any non-RUSD stablecoin */ 
public class FbBusd extends FbErc20 implements IBusd {
	public FbBusd( String address, int decimals, String name) throws Exception {
		super( address, decimals, name);
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	public static String deploy(String filename) throws Exception {
		S.out( "Deploying BUSD from owner" );
		
		return deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

	/** @param callerKey is the name of the Fireblocks wallet */
	@Override public RetVal approve(String callerKey, String spenderAddr, double amt) throws Exception {
		return super.approve( Accounts.instance.getId( callerKey), spenderAddr, amt);
	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production; must have gas 
	 *  @param callerKey is ignored */
	public RetVal mint( String callerKey, String address, double amt) throws Exception {
		return super.mint( Accounts.instance.getId( callerKey), address, amt);
	}
}
