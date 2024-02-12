package fireblocks;

import java.awt.HeadlessException;

import fireblocks.Erc20.Stablecoin;
import reflection.Config;
import tw.util.S;

/** This class represents any non-RUSD stablecoin */ 
public class Busd extends Stablecoin {
	public static void main(String[] args) throws HeadlessException, Exception {
		Config.ask().busd()
			.mint(	Accounts.instance.getAddress("RefWallet"), 
					1000000);
	}
	
	public Busd( String address, int decimals, String name) throws Exception {
		super( address, decimals, name);
	}
	
	/** This can be called by anybody, the BUSD does not have an owner.
	 *  For testing only; cannot be called in production */
	public RetVal mint(String address, double amt) throws Exception {
		return super.mint( Accounts.instance.getId( "Owner"), address, amt);
	}
	
	/** For testing only, as we cannot deploy the real stablecoin */
	public void deploy(String filename) throws Exception {
		S.out( "Deploying %s from owner", name() );
		
		m_address = deploy( 
				filename, 
				Accounts.instance.getId( "Owner"), 
				new String[0], 
				new Object[0], 
				"Deploy BUSD"
		);
	}

}
