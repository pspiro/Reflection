package fireblocks;

import tw.util.S;

/** This is just for testing. */  // move into test folder 
public class Busd extends Erc20 {
	static final String mintKeccak = "40c10f19";

	public Busd( String address, int decimals) {
		super( address, 6);
	}
	
	/** This can be called by anybody, the BUSD does not have an owner. */
	public String mint(int accountId, String address, double amt) throws Exception {
		S.out( "Account %s minting %s %s", accountId, amt, address);
		
		String[] paramTypes = { "address", "uint256" };
		
		Object[] params = { 
				address, 
				toBlockchain( amt) 
		};
		
		return Fireblocks.call( accountId, m_address, mintKeccak, paramTypes, params, "BUSD mint");
	}
}
