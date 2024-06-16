package web3;

import common.Util;

public class Busd extends Stablecoin {
	private IBusd m_core;
	private String m_anyKey; // private key or "Admin1" for FB

	public Busd(String address, int decimals, String name, String anyKey, IBusd core) throws Exception {
		super( address, decimals, name);

		Util.require( core != null, "null core");
		
		m_anyKey = anyKey;
		m_core = core;
	}

	/** For testing only; anyone can call this but they must have some gas */
	public RetVal mint( String address, double amount) throws Exception {
		return m_core.mint( m_anyKey, address, amount);
	}

	/** For testing only; currently only Frontend calls approve in production */ 
	public RetVal approve(String approverKey, String spenderAddr, double amt) throws Exception {
		return m_core.approve( approverKey, spenderAddr, amt);
	}
	
	public RetVal transfer(String fromKey, String toAddr, double amt) throws Exception {
		return m_core.transfer( fromKey, toAddr, amt);
	}

	public interface IBusd {
		RetVal mint( String callerKey, String address, double amount) throws Exception;
		RetVal approve( String approverKey, String spenderAddr, double amt) throws Exception;
		RetVal transfer(String fromKey, String toAddr, double amt) throws Exception;
	}
}
